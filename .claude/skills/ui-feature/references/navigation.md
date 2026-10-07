# Decompose Navigation & Cross-Feature Nav

> Examples use `Note` / `Tag` / `User` as the generic `<Entity>`. Substitute your domain.

Navigation is built on Decompose (`com.arkivanov.decompose`). Each `Component`
owns its own UI subtree and lifecycle; navigation is **type-safe** (routes are
`@Serializable sealed class`); state survives **process death** because Decompose's
`StateKeeper` serializes the entire router stack.

## Three layers of navigation

1. **`RootComponent`** in `:shared` — owns the **primary** `StackNavigation<RootRouter>`. Routes between top-level features (`Auth`, `Home`, `Notes`, `Profile`, `NoteDetail`, `Debug`).
2. **`<Feature>Component`** (or `<Feature>RootComponent` — see naming below) in each `:ui-screen-features:<feature>` with more than one screen — owns a private `StackNavigation<<Feature>Router>`. Single-screen features skip the inner stack and just expose one `BaseComponent`.
3. **`DialogComponent`** in `:shared` — owns a **slot** navigator (`SlotNavigation<DialogConfig>`) parallel to the screen stack. (`references/dialogs.md`.)

There is **no** other navigation mechanism. No global event bus for navigation, no Compose Navigation, no Voyager.

## `*Router` sealed classes

All routes are `@Serializable sealed class` in `:ui-screen-features:screen-api`:

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
    @Serializable public data object Overview : ProfileRouter()
    @Serializable public data object Settings : ProfileRouter()
    @Serializable public data class NoteArchive(val initialRange: DateRange) : ProfileRouter()
}
```

Rules:
- **`@Serializable` on every route class.** Non-serializable routes fail at runtime when the stack serializes on process death.
- **All payloads must also be `@Serializable`** (`DateRange`, `NoteMode`, any project enum).
- **No callbacks or non-serializable fields** in route parameters. Pass only data; pass behavior via constructor lambdas at component-creation time.
- Sub-feature routers nest inside the parent: `RootRouter.Profile(value: ProfileRouter)`. Allows deeplinks like `RootRouter.Profile(ProfileRouter.Settings)`.

## `ChildStack` setup

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

- `serializer = RootRouter.serializer()` — Decompose writes/reads router state from `StateKeeper`.
- `initialConfiguration` — required; the route shown when the stack is empty.
- `handleBackButton = true` — wires system back into Decompose's stack pop.
- `key = "..."` — unique within the parent context; the state-keeper key. Forgetting it collides multiple children in the StateKeeper.
- `childFactory = ::createChild` — builds a `Child` (sealed wrapper around `<X>Component`) per `<X>Router` config.

Feature-root stacks use `initialStack = { listOf(initial) }` instead of `initialConfiguration`.

`createChild` maps each route to a `Child` wrapping a constructed `<X>Component`, threading the navigation callbacks (`toHome = viewModel::toHome`, `close = viewModel::onClose`, one per cross-feature destination). The `Child` wrapper:

```kotlin
public sealed class Child(public open val component: BaseComponent<*>) {
    public data class Authorization(override val component: AuthComponent) : Child(component)
    public data class Home(override val component: HomeRootComponent) : Child(component)
    // ... one entry per route
}
```

A `sealed class Child(component: BaseComponent<*>)` lets `RootScreen` pattern-match for animation selection while keeping `component.Render()` callable polymorphically.

## Naming convention (Component vs RootComponent)

- The **default** feature-root name is the bare `<X>Component` (`AuthComponent`, `ProfileComponent`, `NoteDetailComponent`, `DebugComponent`).
- The `<X>RootComponent` form (`HomeRootComponent`, `NotesRootComponent`) is reserved **only** for features whose first sub-screen reuses the feature name (`:home` has a `Home` sub-screen → `HomeRootComponent`; `:notes` has a `Notes` sub-screen → `NotesRootComponent`).
- Owning a private `StackNavigation` is **orthogonal** to this naming: `AuthComponent`/`ProfileComponent`/`NoteDetailComponent` all own internal stacks despite the bare name. `DebugComponent` is the rare single-screen feature with no inner stack.

## Navigation operations

`StackNavigation<T>` provides: `push(config)`, `pop()`, `popWhile(predicate)`, `popTo(index)`, `replaceCurrent(config)`, `replaceAll(configs)`, `bringToFront(config)`.

`RootComponent` typically uses `replaceAll(RootRouter.Auth(AuthRouter.AuthProcess))` on logout, `replaceAll(RootRouter.Home)` after login, `push(...)` for forward nav, `pop()` for back. (There is no `RootRouter.Login` — the auth flow nests under `RootRouter.Auth`.)

## Direction → navigation translation

ViewModels **never** call `navigation.push(...)` directly. They emit `Direction`s; the Component translates them:

```kotlin
// inside RootComponent
override suspend fun eventListener(direction: RootDirection) {
    when (direction) {
        RootDirection.Login -> if (childStack.value.active.instance !is Child.Authorization) {
            navigation.replaceAll(RootRouter.Auth(AuthRouter.AuthProcess))
        }
        RootDirection.Home -> navigation.replaceAll(RootRouter.Home)
        RootDirection.Profile -> navigation.push(RootRouter.Profile(ProfileRouter.Overview))
        RootDirection.Settings -> navigation.push(RootRouter.Profile(ProfileRouter.Settings))
        is RootDirection.NoteDetail -> navigation.push(RootRouter.NoteDetail(direction.mode))
        RootDirection.Back -> navigation.pop()
        RootDirection.Close -> close.invoke()
    }
}
```

Two patterns:
- **Guarded `replaceAll`**: `RootDirection.Login` only replaces if the current top isn't already an `Authorization` child — the token observer emits `Login` on every token-null transition, including the initial one; without the guard the user is bounced back mid-splash.
- **Most `RootDirection` subtypes are `data object`**, not `data class`. They name a destination and the Component picks the concrete sub-route. `NoteDetail` carries a `NoteMode` payload because the same destination resumes different modes.

For sub-feature components, `eventListener` handles two kinds of directions:
1. **Internal** (within the feature): translate to `navigation.push(<Feature>Router.X)` on this feature's own private nav.
2. **External** (to another feature or back to root): call a constructor-injected lambda (`toProfile()`, `back()`) — the parent component handles the actual stack push.

## Cross-feature navigation

Two `:ui-screen-features:*` modules **cannot** import each other. Direct symbol import between feature modules is **forbidden**. The mechanism (full recipe):

1. **Confirm/declare the destination route in `:ui-screen-features:screen-api`** — e.g. `ProfileRouter.NoteArchive(initialRange)`. If it doesn't exist, add it. `Auth`/`Profile` wrap nested routers; leaf root entries (`Home`, `Notes`, `Debug`) are `data object`; payload-carrying ones are `data class`. When the destination exists but takes different params, **add a new subtype** rather than mutating the existing one.

2. **Add a `RootDirection` entry** (in `:shared`'s `RootDirection.kt`, a `public sealed interface`):
   ```kotlin
   public sealed interface RootDirection : BaseDirection {
       public data object Login : RootDirection
       public data object Home : RootDirection
       public data object Profile : RootDirection
       public data class OpenProfileNoteArchive(val initialRange: DateRange) : RootDirection
   }
   ```
   (This template uses the explicit-direction style; `data object` for 1:1 destinations, `data class` for payloads. A general `data class GoTo(val target: RootRouter)` is an alternative.)

3. **Expose the callback on `RootContract` / `RootViewModel`** (both `public` — they cross the iOS XCFramework boundary):
   ```kotlin
   @Immutable
   public interface RootContract {
       public fun toProfileNoteArchive(range: DateRange)
       @Immutable public companion object Empty : RootContract {
           override fun toProfileNoteArchive(range: DateRange) = Unit
       }
   }
   // RootViewModel (public):
   override fun toProfileNoteArchive(range: DateRange) {
       navigateTo(RootDirection.OpenProfileNoteArchive(range))
   }
   ```
   Feature-level ViewModels stay `internal`; only `:shared` root types are public.

4. **Translate the Direction in `RootComponent.eventListener`**:
   ```kotlin
   is RootDirection.OpenProfileNoteArchive -> navigation.push(
       RootRouter.Profile(ProfileRouter.NoteArchive(direction.initialRange))
   )
   ```

5. **Thread the callback through the originating feature** — `RootComponent.createChild` passes `toProfileNoteArchive = viewModel::toProfileNoteArchive` to `HomeRootComponent` (a `public class`); `HomeRootComponent.createChild` threads it to `HomeOverviewComponent(..., toProfileNoteArchive = toProfileNoteArchive)`. Each child component takes the lambda in its constructor.

6. **Use the callback in the originating ViewModel** — `HomeOverviewViewModel.onChartClick(range)` emits `HomeOverviewDirection.OpenNoteArchive(range)`; `HomeOverviewComponent.eventListener` maps it to `toProfileNoteArchive(direction.range)`.

Visual flow:
```
HomeOverviewViewModel.onChartClick(range)
  → navigateTo(HomeOverviewDirection.OpenNoteArchive(range))
    → HomeOverviewComponent.eventListener → toProfileNoteArchive(range)
      → RootViewModel.toProfileNoteArchive(range)
        → navigateTo(RootDirection.OpenProfileNoteArchive(range))
          → RootComponent.eventListener → navigation.push(RootRouter.Profile(ProfileRouter.NoteArchive(range)))
            → ProfileComponent.createChild — opens the NoteArchive screen
```

Verbose but **explicit** — no shared global event bus, no service locator. The dependency graph between features is visible.

For result-back navigation (feature A opens B, expects a result), use `ResultManager` (`references/results.md`).

### Cross-feature common mistakes (MUST avoid)

- **Direct feature-to-feature import.** Forbidden. Use `screen-api` + callbacks.
- **Lambda parameter in a `Router` payload.** Routes are `@Serializable`; lambdas aren't.
- **Skipping the `RootDirection` step.** Calling `RootRouter.push(...)` directly from a feature VM has no path.
- **Forgetting to thread the callback** through every layer (`createChild` → child component constructor → child VM).
- **Adding a `toX(...)` on `<Feature>Contract`** when it could be a constructor lambda. `Contract` is for the UI's own callbacks, not cross-feature routing.

## Animations

```kotlin
ChildStack(
    modifier = Modifier.fillMaxSize(),
    stack = component.childStack,
    animation = stackAnimation(selector = { child, _, _, _ -> child.instance.animator() }),
    content = { child -> child.instance.component.Render() },
)

private fun RootComponent.Child.animator(): StackAnimator = when (this) {
    is RootComponent.Child.Authorization -> fade()
    is RootComponent.Child.Home -> fade()
    else -> platformStackAnimator()
}
```

The dialog overlay is **not** rendered inside `RootScreen`. `RootComponent.Render()` calls `RootScreen(...)` and `dialogComponent.Render()` as siblings inside `AppTheme { ... }`:

```kotlin
AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
    RootScreen(this, state.value, loaders.value, viewModel)
    dialogComponent.Render()
}
```

`platformStackAnimator()` is `expect/actual` in `:ui-core:foundation` (`fade() + slide()` on Android, `iosLikeSlide()` on iOS). See `references/base-classes.md` § Platform helpers.

## Deeplinks

```kotlin
public enum class Deeplink(public val key: String) {
    NoteEditor(key = "note_editor"),
    NoteArchive(key = "note_archive");
    public companion object {
        public fun fromKey(key: String): Deeplink? = entries.find { it.key == key }
    }
}
```

The deeplink is carried as a raw `String` (notification extras' key) and resolved to `Deeplink` only inside `RootViewModel.parseDeeplink`.

Android wiring: `MainActivity` lifts `intent.getStringExtra(LocalNotificationExtras.DEEPLINK)` into `RootComponent(deeplink = ...)` (cold start) and routes `onNewIntent` extras via `root.handleDeeplink(it)` (warm start).

```kotlin
public fun handleDeeplink(deeplink: String) {
    if (childStack.value.active.configuration is RootRouter.Home) {
        viewModel.applyDeeplink(deeplink)     // warm: apply immediately
    } else {
        viewModel.enqueueDeeplink(deeplink)   // cold: queue for toHome()
    }
}

// RootViewModel
internal fun enqueueDeeplink(deeplink: String) { update { it.copy(deeplink = deeplink) } }
internal fun applyDeeplink(deeplink: String) { parseDeeplink(deeplink)?.let { navigateTo(it) } }
private fun parseDeeplink(raw: String): RootDirection? = when (Deeplink.fromKey(raw)) {
    Deeplink.NoteEditor -> RootDirection.NoteDetail(NoteMode.Draft)
    Deeplink.NoteArchive -> RootDirection.OpenProfileNoteArchive(DateRangePresets.last30Days())
    null -> null
}
```

The cold-start path stores the raw string in `RootState.deeplink`; `toHome()` later drains it after the Home child becomes active.

## Back handling

Default: the system back press goes to Decompose's `BackHandler`, which pops the current stack's top. To override per-screen:

```kotlin
init { backHandler.register(BackCallback(onBack = viewModel::onBack)) }
```

`viewModel.onBack()` is a `Contract` method; it can do custom logic (e.g. clear form state) before calling `navigateTo(Direction.Back)`.

## Anti-patterns (MUST avoid)

- **`LaunchedEffect(Unit) { navigate(...) }`** — forbidden. Navigation goes via `Direction` + `eventListener`.
- **Compose Navigation alongside Decompose** — forbidden. One nav library per project.
- **Sharing a `StackNavigation` across components** — forbidden. Each component owns its own nav source.
- **Mutable routes** — routes are `data class` / `data object`. No `var` fields.
- **Routes carrying lambdas or non-serializable types** — they won't survive process death.
