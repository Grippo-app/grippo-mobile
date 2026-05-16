# Decompose Navigation

Navigation is built on Decompose (`com.arkivanov.decompose`). Each `Component` is the owner of its own UI subtree and its own lifecycle; navigation is **type-safe** (routes are `@Serializable sealed class`); state survives **process death** because Decompose's `StateKeeper` serializes the entire router stack.

## Three layers of navigation

1. **`RootComponent`** in `:shared` — owns the **primary** `StackNavigation<RootRouter>`. Routes between top-level features (`Auth`, `Home`, `Notes`, `Profile`, `NoteDetail`, `Debug`).
2. **`<Feature>Component`** (or `<Feature>RootComponent` — see naming exception below) in each `:ui-screen-features:<feature>` that has more than one screen — owns its own private `StackNavigation<<Feature>Router>`. Routes between sub-screens of one feature. Single-screen features (e.g. `:debug`) skip the inner stack and just expose one `BaseComponent`.
3. **`DialogComponent`** in `:shared` — owns a **slot** navigator (`SlotNavigation<DialogConfig>`) parallel to the screen stack. See `03-architecture-patterns/03-dialog-navigation.md`.

There is **no** other navigation mechanism. No global event bus for navigation, no Compose Navigation, no Voyager.

## `*Router` sealed classes

All routes are declared as `@Serializable sealed class` in `:ui-screen-features:screen-api`:

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data class Auth(val value: AuthRouter) : RootRouter()
    @Serializable public data object Home : RootRouter()
    @Serializable public data object Notes : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
    @Serializable public data class NoteDetail(val mode: NoteMode) : RootRouter()
    @Serializable public data object Debug : RootRouter()
}

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable public data object Body : ProfileRouter()
    @Serializable public data object Settings : ProfileRouter()
    @Serializable public data class NoteArchive(val initialRange: DateRange) : ProfileRouter()
}

@Serializable
public sealed class HomeRouter : BaseRouter {
    @Serializable public data object Home : HomeRouter()
}
```

Rules:

- **`@Serializable` on every route class.** Decompose serializes the stack on process death; non-serializable routes will fail at runtime.
- **All payloads must also be `@Serializable`.** `DateRange`, `NoteMode`, any project enum — all `@Serializable`.
- **No callbacks or non-serializable fields** in route parameters. Pass only data; pass behavior via constructor lambdas at component-creation time.
- Sub-feature routers nest inside the parent: `RootRouter.Profile(value: ProfileRouter)`. This allows deeplinks like "open Profile → Settings" to be expressed as a single `RootRouter.Profile(ProfileRouter.Settings)` config.

## `ChildStack` setup

Inside `RootComponent`:

```kotlin
private val navigation = StackNavigation<RootRouter>()

internal val childStack: Value<ChildStack<RootRouter, Child>> = childStack(
    source = navigation,
    serializer = RootRouter.serializer(),
    initialConfiguration = RootRouter.Auth(AuthRouter.Splash),
    handleBackButton = true,
    key = "RootComponent",
    childFactory = ::createChild,
)
```

Key points:

- `serializer = RootRouter.serializer()` — Decompose uses this to write/read the router state from `StateKeeper`.
- `initialConfiguration` — the route shown when the stack is empty. Required. The shell typically opens on the auth/splash entrypoint and switches to `Home` once the token check completes.
- `handleBackButton = true` — wires system back into Decompose's stack pop. Combined with the explicit `BackCallback` (see "Back handling"), this is what makes hardware back navigate correctly.
- `key = "RootComponent"` — unique within the parent context. Used as the state-keeper key.
- `childFactory = ::createChild` — function that builds a `Child` (sealed wrapper around `<X>Component`) for each `<X>Router` config.

`createChild`:

```kotlin
private fun createChild(router: RootRouter, context: ComponentContext): Child = when (router) {
    is RootRouter.Auth -> Child.Authorization(
        AuthComponent(
            componentContext = context,
            initial = router.value,
            toHome = viewModel::toHome,
            close = viewModel::onClose,
        )
    )

    RootRouter.Home -> Child.Home(
        HomeRootComponent(
            componentContext = context,
            initial = HomeRouter.Home,
            toBody = viewModel::toNoteArchive,
            toNoteDetail = viewModel::toNoteDetail,
            toNotes = viewModel::toNotes,
            // ... one callback per cross-feature destination
            close = viewModel::onClose,
        )
    )

    RootRouter.Notes -> Child.Notes(
        NotesRootComponent(
            componentContext = context,
            initial = NotesRouter.Notes,
            toNoteDetail = viewModel::toNoteDetail,
            close = viewModel::onBack,
        )
    )

    is RootRouter.Profile -> Child.Profile(
        ProfileComponent(
            componentContext = context,
            initial = router.value,
            close = viewModel::onBack,
        )
    )

    is RootRouter.NoteDetail -> Child.NoteDetail(
        NoteDetailComponent(
            componentContext = context,
            initial = NoteDetailRouter.Edit(router.mode),
            close = viewModel::onBack,
        )
    )

    is RootRouter.Debug -> Child.Debug(
        DebugComponent(componentContext = context, close = viewModel::onBack)
    )
}
```

The `Child` wrapper:

```kotlin
public sealed class Child(public open val component: BaseComponent<*>) {
    public data class Authorization(override val component: AuthComponent) : Child(component)
    public data class Home(override val component: HomeRootComponent) : Child(component)
    public data class Notes(override val component: NotesRootComponent) : Child(component)
    public data class Profile(override val component: ProfileComponent) : Child(component)
    public data class NoteDetail(override val component: NoteDetailComponent) : Child(component)
    public data class Debug(override val component: DebugComponent) : Child(component)
}
```

Note the naming convention: the **default** feature-root name is the bare `<X>Component` (e.g. `AuthComponent`, `ProfileComponent`, `NoteDetailComponent`, `DebugComponent`). The `<X>RootComponent` form (`HomeRootComponent`, `NotesRootComponent`) is reserved for features whose first sub-screen reuses the feature name — `:home` has a `Home` sub-screen, `:notes` has a `Notes` sub-screen, so the parent gets a `Root` suffix to avoid the collision. Owning a private `StackNavigation<<X>Router>` is **orthogonal** to this naming choice: `AuthComponent`, `ProfileComponent`, `NoteDetailComponent` all own internal stacks despite the bare name; `DebugComponent` is the rare single-screen feature with no inner stack. All are `BaseComponent<DIRECTION>`.

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
- `navigation.replaceAll(RootRouter.Auth(AuthRouter.AuthProcess))` on logout (there is no `RootRouter.Login` — the auth flow nests under `RootRouter.Auth`).
- `navigation.replaceAll(RootRouter.Home)` after successful login.
- `navigation.push(RootRouter.Profile(...))` for forward nav.
- `navigation.pop()` for back.

## Direction → navigation translation

ViewModels never call `navigation.push(...)` directly. They emit `Direction`s; the Component translates them:

```kotlin
// inside RootComponent
override suspend fun eventListener(direction: RootDirection) {
    when (direction) {
        RootDirection.Login -> if (childStack.value.active.instance !is Child.Authorization) {
            navigation.replaceAll(RootRouter.Auth(AuthRouter.AuthProcess))
        }
        RootDirection.Home -> navigation.replaceAll(RootRouter.Home)
        RootDirection.Profile -> navigation.push(RootRouter.Profile(ProfileRouter.Body))
        RootDirection.Settings -> navigation.push(RootRouter.Profile(ProfileRouter.Settings))
        is RootDirection.NoteDetail -> navigation.push(RootRouter.NoteDetail(direction.stage))
        RootDirection.Back -> navigation.pop()
        RootDirection.Close -> close.invoke()
    }
}
```

Two patterns to notice:

- **Guarded `replaceAll`**: `RootDirection.Login` only replaces the stack if the current top isn't already an `Authorization` child. The token observer in `RootViewModel` emits `Login` on every token-null transition, including the initial one when the shell already started on `Auth(AuthRouter.Splash)` — without the guard the user would be bounced back to `AuthProcess` mid-splash.
- **Most `RootDirection` subtypes are `data object`**, not `data class`. They name a destination (`Settings`, `Profile`, …) and the Component picks the concrete sub-route to push (e.g. `ProfileRouter.Body`). `NoteDetail` is the exception — it carries a `NoteMode` payload because the same destination can resume different modes.

For sub-feature components, `eventListener` typically handles two kinds of directions:

1. **Internal** (within the feature): translate to `navigation.push(<Feature>Router.X)` on this feature's own private nav.
2. **External** (to another feature or back to root): call a constructor-injected lambda (`toProfile()`, `back()`) — the parent component handles the actual stack push.

## Cross-feature navigation

A `:ui-screen-features:home` module cannot directly navigate to a screen in `:ui-screen-features:profile`. The mechanism:

1. Declare the route in `:ui-screen-features:screen-api` (`ProfileRouter.NoteArchive(initialRange)`).
2. Add `RootDirection.OpenNoteArchive(range: DateRange)` (or compose: `RootDirection.Profile(ProfileRouter.NoteArchive(range))`).
3. `RootViewModel` exposes a callback: `fun toNoteArchive(range: DateRange) { navigateTo(RootDirection.OpenNoteArchive(range)) }`.
4. `RootComponent.createChild` for the Home child threads the callback through: `HomeRootComponent(..., toNoteArchive = viewModel::toNoteArchive)`.
5. `HomeRootComponent` accepts it and threads to its sub-components: `HomeOverviewComponent(..., toNoteArchive = toNoteArchive)`.
6. `HomeOverviewViewModel.onChartClick(range)` calls the constructor lambda.

This is verbose but **explicit**. The dependency graph between features is visible.

## Animations

Per-child stack animations in the screen:

```kotlin
@Composable
internal fun RootScreen(
    component: RootComponent,
    state: RootState,
    loaders: ImmutableSet<RootLoader>,
    contract: RootContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    ChildStack(
        modifier = Modifier.fillMaxSize(),
        stack = component.childStack,
        animation = stackAnimation(
            selector = { child, _, _, _ -> child.instance.animator() }
        ),
        content = { child -> child.instance.component.Render() },
    )
}

private fun RootComponent.Child.animator(): StackAnimator = when (this) {
    is RootComponent.Child.Authorization -> fade()
    is RootComponent.Child.Home -> fade()
    is RootComponent.Child.Debug -> platformStackAnimator()
    is RootComponent.Child.Profile -> platformStackAnimator()
    is RootComponent.Child.NoteDetail -> platformStackAnimator()
    is RootComponent.Child.Notes -> platformStackAnimator()
}
```

The dialog overlay is **not** rendered inside `RootScreen`. `RootComponent.Render()` calls `RootScreen(...)` and `dialogComponent.Render()` as siblings inside `AppTheme { ... }`:

```kotlin
AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
    RootScreen(this, state.value, loaders.value, viewModel)
    dialogComponent.Render()
}
```

This keeps the screen stack purely about screens; the dialog slot lives one layer up in the composable hierarchy.

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
    NoteDraft(key = "note_draft"),
    NoteArchive(key = "note_archive");

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
    Deeplink.NoteDraft -> RootDirection.NoteDetail(NoteMode.Draft)
    Deeplink.NoteArchive -> RootDirection.NoteArchive
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
