# Decompose Navigation & Cross-Feature Nav

Every route, Direction, Component and deeplink named below exists in the current
source; inspect the file before relying on a detail, since code owns signatures.

Navigation is built on Decompose (`com.arkivanov.decompose`). Each `Component`
owns its own UI subtree and lifecycle; screen navigation is **type-safe** (routes are
`@Serializable sealed class`). Decompose's `StateKeeper` serializes the screen
router stack for restoration where the platform shell supplies saved state;
see [process-death.md](process-death.md) for platform limits. Dialog sessions use
in-memory configs and are not restored.

## Three layers of navigation

1. **RootComponent** in shared owns the primary StackNavigation<RootRouter>.
   Inspect `RootComponent.createChild`; it builds `AuthComponent`, `MainComponent`
   (whose `createHome`/`createCalendar`/`createLibrary`/`createProfile` factories
   build `HomeRootComponent`, `TrainingsRootComponent`, `ExercisesRootComponent`
   with `ExercisesRouter.Library(ExercisesRouter.Mode.Browse)` and
   `ProfileComponent` for the `MainRouter` tabs), `TrainingsRootComponent`,
   `ProfileComponent`, `DebugComponent` and `TrainingComponent`.
   `MainComponent`'s Library tab uses `Mode.Browse` without a result key.
   [TrainingComponent](../../../../ui-screen-features/training/src/commonMain/kotlin/com/grippo/training/TrainingComponent.kt)
   receives its `createExercises` factory from the same root for picking modes
   `Mode.Default` / `Mode.SimilarTo`, each carrying a `resultKey`, while keeping
   the picker in its own stack.
   `OnboardingComponent` is an internal child of `AuthComponent`.
2. **`<Feature>Component`** (or `<Feature>RootComponent` — see naming below) in each `:ui-screen-features:<feature>` with more than one screen — owns a private `StackNavigation<<Feature>Router>`. Single-screen features skip the inner stack and just expose one `BaseComponent`.
3. **`DialogComponent`** in `:shared` — owns `SlotNavigation<DialogSession>` parallel to the screen stack. `DialogContentComponent` owns the internal `StackNavigation<DialogStep>` for that session. Both use `serializer = null`; configs and typed callbacks remain live in memory. See [dialogs.md](dialogs.md).

There is **no** other navigation mechanism. No global event bus for navigation, no Compose Navigation, no Voyager.

An ordinary screen ViewModel uses `DialogController.open(config)` to request a new
sheet session; [dialog retention and opening order](dialogs.md#dialogcontroller--dialogprovider)
govern replacement and pending errors. The host supplies the
originating step's `DialogController.Session` for nested `push`,
`replaceCurrent`, `back` and `close`; it ignores inactive or removed steps.
Nested features emit semantic directions and forward typed constructor callbacks;
the host chooses the config/title and connects results. Do not inject a Session
from Koin or serialize it with a screen route.

## `*Router` sealed classes

All routes are `@Serializable sealed class` in `:ui-screen-features:screen-api`.
`RootRouter` as currently shipped:

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data class Auth(val value: AuthRouter) : RootRouter()
    @Serializable public data object Trainings : RootRouter()
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
    @Serializable public data object Debug : RootRouter()
    @Serializable public data class Training(val stage: StageState) : RootRouter()
}
```

Nested routers (inspect the files for annotations such as `@JsonIgnoreUnknownKeys`):
`AuthRouter` = `Splash`, `Onboarding`, `AuthProcess(flowId: FlowId = FlowId.create())`
(a data class — construct it as `AuthRouter.AuthProcess()`), `ProfileCreation`;
`ProfileRouter` = `Menu`, `Equipments`, `Muscles`, `Body`, `Experience`, `Settings`,
`Social`, `Goal`. `StageState` (`Add`, `Edit(id)`, `Draft`) lives in `:ui-core:state`,
package `com.grippo.core.state.stage`.

Rules:
- **`@Serializable` on every route class.** Non-serializable routes fail at runtime when the stack serializes on process death.
- **All payloads must also be `@Serializable`** (`DateRange`, `StageState`, `FlowId`, any project enum). One exception by design: a `ResultKey` field serializes through `ResultKeySerializer` as its address only, so the nested `Action` protocol it points to (`TrainingRouter.Exercise.Action`) is not `@Serializable`; see [results.md](results.md).
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
    public data class Home(override val component: MainComponent) : Child(component)
    // ... Trainings, Profile, Debug, Training — one entry per route
}
```

A `sealed class Child(component: BaseComponent<*>)` lets `RootScreen` pattern-match for animation selection while keeping `component.Render()` callable polymorphically.

## Naming convention (Component vs RootComponent)

Feature-root names and visibility follow
[module structure](module-structure.md#ui-screen-featuresfeature).

## Returning data from a screen

Read [results.md](results.md) for the agreed return convention. The receiving
screen owns companion key definitions; the producing route carries explicit
`resultKey: ResultKey<Result<Payload>>`. Thread it without rewriting through Directions, constructor
callbacks and `createChild`. The producer sends the result then requests ordinary
Back. Parent navigation performs its normal `pop()`; receiver observers handle
data and request their own next destination through normal Directions.

Do not infer the receiver from `childStack`, add an `Origin` mapping in the
producer, or introduce a parent close-and-resend relay or runtime selection flag.
Keep callbacks out of serialized routes and retain keys on restoration.

## Navigation operations

`StackNavigation<T>` provides: `push(config)`, `pop()`, `popWhile(predicate)`, `popTo(index)`, `replaceCurrent(config)`, `replaceAll(configs)`, `bringToFront(config)`.

`RootComponent` typically uses `replaceAll(RootRouter.Auth(AuthRouter.AuthProcess()))` on logout, `replaceAll(RootRouter.Home)` after login, `push(...)` for forward nav, `pop()` for back. (There is no `RootRouter.Login` — the auth flow nests under `RootRouter.Auth`.)

## Direction → navigation translation

ViewModels **never** call `navigation.push(...)` directly. They emit `Direction`s; the Component translates them:

```kotlin
// inside RootComponent (abbreviated; inspect RootComponent.kt for every branch)
override suspend fun eventListener(direction: RootDirection) {
    when (direction) {
        RootDirection.Login -> {
            if (childStack.value.active.configuration !is RootRouter.Auth) {
                navigation.replaceAll(RootRouter.Auth(AuthRouter.AuthProcess()))
            }
        }
        RootDirection.Home -> navigation.replaceAll(RootRouter.Home)
        RootDirection.Goal -> navigation.push(RootRouter.Profile(ProfileRouter.Goal))
        RootDirection.WeightHistory -> navigation.push(RootRouter.Profile(ProfileRouter.Body))
        is RootDirection.Training -> navigation.push(RootRouter.Training(direction.stage))
        RootDirection.Back -> navigation.pop()
        RootDirection.Close -> close.invoke()
        // ... Debug, Trainings
    }
}
```

Inspect `shared/src/commonMain/kotlin/com/grippo/shared/root/RootDirection.kt`
for the current destinations and their payloads.

Two patterns:
- **Session-loss ownership**: Root checks only its own route; `RootDirection.Login` replaces
  a non-Auth root with AuthProcess. ProfileCreation's retained ViewModel observes its own
  session prerequisite and emits the existing `ToLogin` direction through constructor callbacks.
  Parent Components do not inspect a child's internal stack or expose forwarding methods to
  enforce that prerequisite. Initial null tokens preserve Splash, Onboarding and AuthProcess;
  restored ProfileCreation also leaves through `ToLogin` when its session is absent.
- **Most `RootDirection` subtypes are `data object`**, not `data class`. They name a destination and the Component picks the concrete sub-route. `Training` carries a `StageState` payload (`Add`, `Edit(id)`, `Draft`) because the same destination opens different stages.

For sub-feature components, `eventListener` handles two kinds of directions:
1. **Internal** (within the feature): translate to `navigation.push(<Feature>Router.X)` on this feature's own private nav.
2. **External** (to another feature or back to root): call a constructor-injected lambda (`toGoal()`, `back()`) — the parent component handles the actual stack push.

## Cross-feature navigation

Two `:ui-screen-features:*` modules **cannot** import each other. A destination
owned by the root stack uses the callback chain below, shown on the shipped
Home → Profile/Goal jump (the "add goal" action on the Home dashboard).
Embedded flows follow [nested cross-feature flows](#nested-cross-feature-flows).

The `toX` names below describe existing root/container relay APIs, not a naming
exception for new callbacks; [code style](../../validation-gates/references/code-style.md)
owns naming. Inspect and reuse the actual chain before adding a destination.

1. **Confirm/declare the destination route in `:ui-screen-features:screen-api`** — here `ProfileRouter.Goal`. If it doesn't exist, add it. `Auth`/`Profile` wrap nested routers; leaf root entries (`Home`, `Trainings`, `Debug`) are `data object`; payload-carrying ones (`Training(stage)`) are `data class`. When destination inputs change, inspect all callers and archived-route compatibility, including supported invalidation when archives become incompatible. Reuse the existing subtype for the same destination when a coherent migration is within scope; add a subtype only for a distinct destination or contract.

2. **Add a `RootDirection` entry** (in `:shared`'s `RootDirection.kt`, a `public sealed interface`): `public data object Goal : RootDirection`. Grippo uses the explicit-direction style: `data object` for 1:1 destinations, `data class` for payloads (`Training(val stage: StageState)`). Do not introduce a generic GoTo route wrapper.

3. **Expose the callback on `RootContract` / `RootViewModel`** (both `public` — they cross the iOS XCFramework boundary; abbreviated):
   ```kotlin
   @Immutable
   public interface RootContract {
       public fun toGoal()
       @Immutable public companion object Empty : RootContract {
           override fun toGoal() {}
       }
   }
   // RootViewModel (public):
   override fun toGoal() {
       navigateTo(RootDirection.Goal)
   }
   ```
   Feature-level leaf ViewModels stay `internal`; `:shared` root types are public.

4. **Translate the Direction in `RootComponent.eventListener`**:
   ```kotlin
   RootDirection.Goal -> navigation.push(
       RootRouter.Profile(ProfileRouter.Goal)
   )
   ```

5. **Thread the callback through the originating feature** — `RootComponent.createChild` builds `MainComponent` and passes it a `createHome` factory that constructs `HomeRootComponent(..., toGoal = viewModel::toGoal, ...)` (a `public class`); `HomeRootComponent.createChild` threads it to `HomeComponent(..., toGoal = viewModel::toGoal, ...)` through `HomeRootViewModel.toGoal()` / `HomeRootDirection.Goal`. Each child component takes the lambda in its constructor.

6. **Use the callback in the originating ViewModel** — `HomeViewModel.onAddGoal()` emits `HomeDirection.Goal`; `HomeComponent.eventListener` maps it to `toGoal.invoke()`.

Visual flow:
```
HomeViewModel.onAddGoal() → navigateTo(HomeDirection.Goal)
  → HomeComponent.eventListener → toGoal()
    → HomeRootViewModel.toGoal() → navigateTo(HomeRootDirection.Goal)
      → HomeRootComponent.eventListener → toGoal()   // RootViewModel::toGoal, supplied by RootComponent's createHome factory
        → RootViewModel.toGoal() → navigateTo(RootDirection.Goal)
          → RootComponent.eventListener → navigation.push(RootRouter.Profile(ProfileRouter.Goal))
            → ProfileComponent.createChild — opens the Goal screen
```

A Direction with no emitting callback is dead code: when a screen loses the control that emitted it, remove the Direction and its callback through the whole chain in the same change rather than leaving a mapped but unreachable branch.

Verbose but **explicit** — no shared global event bus, no service locator. The dependency graph between features is visible.

For result-back navigation (feature A opens B, expects a result), use `ResultManager` (`references/results.md`).

### Nested cross-feature flows

When a foreign feature belongs inside the host's stack or tabs, the host owns
navigation and `shared` supplies a constructor factory for the child. Inspect
`MainComponent`'s tab factories and `TrainingComponent.createExercises` with their
wiring in `RootComponent`.

The host invokes the factory from its child factory with the child
`ComponentContext`, typed route inputs and its ordinary Back/close callback.
The factory returns `BaseComponent<*>`; `shared` selects the concrete feature
class. The host depends on `screen-api` and foundation, and renders the child
through `Render()`. Its existing stack, route serializer and context keys retain
their ownership. Factories stay in constructors; saved routes contain data and
result keys. Screen results follow [the existing result protocol](results.md).

### Cross-feature common mistakes (MUST avoid)

- **Direct feature-to-feature import.** Forbidden. Use `screen-api` + callbacks.
- **Lambda parameter in a `Router` payload.** Routes are `@Serializable`; lambdas aren't.
- **Skipping `RootDirection` for a root-stack transition.** A feature requests
  that transition through its constructor callback; `RootComponent` owns the push.
- **Forgetting to thread the callback** through every layer (`createChild` → child component constructor → its eventListener).
- **Adding a routing callback to a leaf UI Contract instead of emitting its
  normal Direction.** The leaf Component invokes its constructor callback.
  Container/root ViewModels may relay child callbacks through their existing
  typed `toX` methods and Directions, as the recipe above shows.

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
    is RootComponent.Child.Debug -> platformStackAnimator()
    is RootComponent.Child.Profile -> platformStackAnimator()
    is RootComponent.Child.Training -> platformStackAnimator()
    is RootComponent.Child.Trainings -> platformStackAnimator()
}
```

The dialog overlay is **not** rendered inside `RootScreen`. `RootComponent.Render()` reads `AppTheme.current`/`AppLocale.current`, installs locale-aware date formatting, then calls `RootScreen(...)` and `dialogComponent.Render()` as siblings inside `AppTheme { ... }`:

```kotlin
LaunchedEffect(systemLocaleTag) {
    DateFormatting.install(systemLocaleTag)
}

AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
    RootScreen(this, state.value, loaders.value, viewModel)
    dialogComponent.Render()
}
```

`platformStackAnimator()` is `expect/actual` in `:ui-core:foundation` (`fade() + slide()` on Android, `iosLikeSlide()` on iOS). See `references/base-classes.md` § Platform helpers.

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
internal fun enqueueDeeplink(deeplink: String) {
    update { it.copy(deeplink = deeplink) }
}

internal fun applyDeeplink(deeplink: String) {
    parseDeeplink(deeplink)?.let { navigateTo(it) }
}

private fun parseDeeplink(raw: String): RootDirection? = when (Deeplink.fromKey(raw)) {
    Deeplink.TrainingDraft -> RootDirection.Training(StageState.Draft)
    Deeplink.WeightHistory -> RootDirection.WeightHistory
    null -> null
}
```

The cold-start path stores the raw string in `RootState.deeplink`; `toHome()` emits Home and the parsed direction consecutively, then clears the stored value. It does not wait for the Home child to become active. The navigator is conflated; these emissions do not establish ordered or exactly-once delivery.

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
