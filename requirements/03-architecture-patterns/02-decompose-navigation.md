# Decompose Navigation

Navigation is built on Decompose (`com.arkivanov.decompose`). Each `Component` is the owner of its own UI subtree and its own lifecycle; navigation is **type-safe** (routes are `@Serializable sealed class`); state survives **process death** because Decompose's `StateKeeper` serializes the entire router stack.

## Three layers of navigation

1. **`RootComponent`** in `:shared` — owns the **primary** `StackNavigation<RootRouter>`. Routes between top-level features (`Authorization`, `Home`, `Profile`, `Training`).
2. **`<Feature>RootComponent`** in each `:ui-screen-features:<feature>` — owns its own private `StackNavigation<<Feature>Router>`. Routes between sub-screens of one feature.
3. **`DialogComponent`** in `:shared` — owns a **slot** navigator (`SlotNavigation<DialogConfig>`) parallel to the screen stack. See `03-architecture-patterns/03-dialog-navigation.md`.

There is **no** other navigation mechanism. No global event bus for navigation, no Compose Navigation, no Voyager.

## `*Router` sealed classes

All routes are declared as `@Serializable sealed class` in `:ui-screen-features:screen-api`:

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data object Authorization : RootRouter()
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
    @Serializable public data class Training(val stage: StageState) : RootRouter()
    @Serializable public data object Debug : RootRouter()
}

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable public data object Body : ProfileRouter()
    @Serializable public data object Settings : ProfileRouter()
    @Serializable public data class WorkoutHistory(val initialRange: DateRange) : ProfileRouter()
}

@Serializable
public sealed class HomeRouter : BaseRouter {
    @Serializable public data object Overview : HomeRouter()
    @Serializable public data object Stats : HomeRouter()
}
```

Rules:

- **`@Serializable` on every route class.** Decompose serializes the stack on process death; non-serializable routes will fail at runtime.
- **All payloads must also be `@Serializable`.** `DateRange`, `StageState`, any product enum — all `@Serializable`.
- **No callbacks or non-serializable fields** in route parameters. Pass only data; pass behavior via constructor lambdas at component-creation time.
- Sub-feature routers nest inside the parent: `RootRouter.Profile(value: ProfileRouter)`. This allows deeplinks like "open Profile → Settings" to be expressed as a single `RootRouter.Profile(ProfileRouter.Settings)` config.

## `ChildStack` setup

Inside `RootComponent`:

```kotlin
private val navigation = StackNavigation<RootRouter>()

private val stack: Value<ChildStack<RootRouter, Child>> = childStack(
    source = navigation,
    serializer = RootRouter.serializer(),
    initialConfiguration = RootRouter.Home,
    key = "RootComponent",
    childFactory = ::createChild,
)
```

Key points:

- `serializer = RootRouter.serializer()` — Decompose uses this to write/read the router state from `StateKeeper`.
- `initialConfiguration` — the route shown when the stack is empty. Required.
- `key = "RootComponent"` — unique within the parent context. Used as the state-keeper key.
- `childFactory = ::createChild` — function that builds a `Child` (sealed wrapper around `<X>Component`) for each `<X>Router` config.

`createChild`:

```kotlin
private fun createChild(config: RootRouter, ctx: ComponentContext): Child = when (config) {
    is RootRouter.Authorization ->
        Child.Authorization(AuthorizationRootComponent(ctx, back = viewModel::onBack))

    is RootRouter.Home ->
        Child.Home(HomeRootComponent(
            componentContext = ctx,
            toProfile = { viewModel.toProfile() },
            toTraining = { stage -> viewModel.toTraining(stage) },
        ))

    is RootRouter.Profile ->
        Child.Profile(ProfileRootComponent(
            componentContext = ctx,
            initialRoute = config.value,
            back = viewModel::onBack,
        ))

    is RootRouter.Training ->
        Child.Training(TrainingRootComponent(ctx, config.stage, back = viewModel::onBack))

    is RootRouter.Debug ->
        Child.Debug(DebugRootComponent(ctx, back = viewModel::onBack))
}
```

The `Child` wrapper:

```kotlin
sealed class Child(open val component: BaseComponent<*>) {
    data class Authorization(override val component: AuthorizationRootComponent) : Child(component)
    data class Home(override val component: HomeRootComponent) : Child(component)
    data class Profile(override val component: ProfileRootComponent) : Child(component)
    data class Training(override val component: TrainingRootComponent) : Child(component)
    data class Debug(override val component: DebugRootComponent) : Child(component)
}
```

A `sealed class Child(component: BaseComponent<*>)` is the canonical pattern — it lets `RootScreen` pattern-match for animation selection while keeping `component.Render()` callable polymorphically.

## Navigation operations

The `StackNavigation<T>` interface provides:

| Method | When |
|---|---|
| `push(config)` | Forward navigation; adds to stack top |
| `pop()` | Back navigation; pops top |
| `popWhile(predicate)` | Pop until predicate matches |
| `popTo(index)` | Pop to a specific index |
| `replaceCurrent(config)` | Replace top |
| `replaceAll(configs)` | Clear stack and set new |
| `bringToFront(config)` | Move existing instance to top, or push |

`RootComponent` typically uses:
- `navigation.replaceAll(RootRouter.Login)` on logout.
- `navigation.replaceAll(RootRouter.Home)` after successful login.
- `navigation.push(RootRouter.Profile(...))` for forward nav.
- `navigation.pop()` for back.

## Direction → navigation translation

ViewModels never call `navigation.push(...)` directly. They emit `Direction`s; the Component translates them:

```kotlin
// inside RootComponent
override suspend fun eventListener(direction: RootDirection) {
    when (direction) {
        RootDirection.Login -> navigation.replaceAll(RootRouter.Authorization)
        RootDirection.Home -> navigation.replaceAll(RootRouter.Home)
        is RootDirection.Profile -> navigation.push(RootRouter.Profile(direction.value))
        is RootDirection.Training -> navigation.push(RootRouter.Training(direction.stage))
        RootDirection.Back -> navigation.pop()
    }
}
```

For sub-feature components, `eventListener` typically handles two kinds of directions:

1. **Internal** (within the feature): translate to `navigation.push(<Feature>Router.X)` on this feature's own private nav.
2. **External** (to another feature or back to root): call a constructor-injected lambda (`toProfile()`, `back()`) — the parent component handles the actual stack push.

## Cross-feature navigation

A `:ui-screen-features:home` module cannot directly navigate to a screen in `:ui-screen-features:profile`. The mechanism:

1. Declare the route in `:ui-screen-features:screen-api` (`ProfileRouter.WorkoutHistory(initialRange)`).
2. Add `RootDirection.OpenWorkoutHistory(range: DateRange)` (or compose: `RootDirection.Profile(ProfileRouter.WorkoutHistory(range))`).
3. `RootViewModel` exposes a callback: `fun toWorkoutHistory(range: DateRange) { navigateTo(RootDirection.OpenWorkoutHistory(range)) }`.
4. `RootComponent.createChild` for the Home child threads the callback through: `HomeRootComponent(..., toWorkoutHistory = viewModel::toWorkoutHistory)`.
5. `HomeRootComponent` accepts it and threads to its sub-components: `HomeOverviewComponent(..., toWorkoutHistory = toWorkoutHistory)`.
6. `HomeOverviewViewModel.onChartClick(range)` calls the constructor lambda.

This is verbose but **explicit**. The dependency graph between features is visible.

## Animations

Per-child stack animations in the screen:

```kotlin
@Composable
internal fun RootScreen(...) {
    ChildStack(
        stack = stack,
        animation = stackAnimation(selector = { child, _, _, _ -> child.instance.animator() })
    ) { child ->
        child.instance.component.Render()
    }
}

private fun RootComponent.Child.animator(): StackAnimator = when (this) {
    is Child.Authorization -> fade()
    is Child.Home -> fade()
    is Child.Profile -> platformStackAnimator()        // iOS-like slide on iOS; fade+slide on Android
    is Child.Training -> platformStackAnimator()
    is Child.Debug -> platformStackAnimator()
}
```

`platformStackAnimator()` is `expect/actual` in `:ui-core:foundation`:

```kotlin
// commonMain
public expect fun platformStackAnimator(): StackAnimator

// androidMain
public actual fun platformStackAnimator(): StackAnimator = (fade() + slide())

// iosMain
public actual fun platformStackAnimator(): StackAnimator = iosLikeSlide()
```

See `04-base-classes/07-platform-helpers.md`.

## Deeplinks

```kotlin
public enum class Deeplink(public val key: String) {
    TrainingDraft(key = "training_draft"),
    WeightHistory(key = "weight_history");

    public companion object {
        public fun fromKey(key: String): Deeplink? = entries.find { it.key == key }
    }
}
```

The deeplink is carried through the system as a raw `String` (the notification extras' key). It is only resolved to a `Deeplink` enum inside `RootViewModel.parseDeeplink`.

Wiring (Android):

```kotlin
// MainActivity
private val root: RootComponent by lazy {
    retainedComponent {
        RootComponent(
            componentContext = it,
            close = ::finishAffinity,
            deeplink = intent.getStringExtra(LocalNotificationExtras.DEEPLINK),
        )
    }
}

// onNewIntent (warm start: app already running, user tapped a notification)
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intent.getStringExtra(LocalNotificationExtras.DEEPLINK)
        ?.let { root.handleDeeplink(it) }
}
```

`RootComponent.handleDeeplink` picks the right path based on the current stack:

```kotlin
public fun handleDeeplink(deeplink: String) {
    if (childStack.value.active.configuration is RootRouter.Home) {
        viewModel.applyDeeplink(deeplink)
    } else {
        viewModel.enqueueDeeplink(deeplink)
    }
}
```

`RootViewModel`:

```kotlin
/** Cold start — queue for consumption when [toHome] is called. */
internal fun enqueueDeeplink(deeplink: String) {
    update { it.copy(deeplink = deeplink) }
}

/** Warm start — already on Home, apply immediately. */
internal fun applyDeeplink(deeplink: String) {
    parseDeeplink(deeplink)?.let { navigateTo(it) }
}

private fun parseDeeplink(raw: String): RootDirection? = when (Deeplink.fromKey(raw)) {
    Deeplink.TrainingDraft -> RootDirection.Training(StageState.Draft)
    Deeplink.WeightHistory -> RootDirection.WeightHistory
    null -> null
}
```

The cold-start path stores the raw string in `RootState.deeplink`; `toHome()` later drains it (`state.value.deeplink?.let { parseDeeplink(it)?.let(::navigateTo) }`) after the Home child becomes active.

## Back handling

Default: the system back press goes to Decompose's `BackHandler`, which pops the current stack's top.

To override per-screen:

```kotlin
internal class FooComponent(
    componentContext: ComponentContext,
    ...
) : BaseComponent<FooDirection>(componentContext) {

    init {
        backHandler.register(BackCallback(onBack = viewModel::onBack))
    }
}
```

`viewModel.onBack()` is a `Contract` method; it can do custom logic (e.g. clear form state) before calling `navigateTo(Direction.Back)`.

## Anti-patterns

- **`LaunchedEffect(Unit) { navigate(...) }`** — forbidden. Navigation goes via `Direction` + `eventListener`.
- **Compose Navigation alongside Decompose** — forbidden. One nav library per project.
- **Sharing a `StackNavigation` across components** — forbidden. Each component owns its own nav source.
- **Mutable routes** — routes are `data class` / `data object`. No `var` fields.
- **Routes carrying lambdas or non-serializable types** — they won't survive process death.
