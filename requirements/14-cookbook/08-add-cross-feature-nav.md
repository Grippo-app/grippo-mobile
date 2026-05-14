# Add Cross-Feature Navigation

How to navigate from a screen in one `:ui-screen-features:*` module to a screen in another — e.g. open a profile screen from a home screen.

Direct feature-to-feature imports are **forbidden**. The mechanism uses `:ui-screen-features:screen-api` for type-safe routes and constructor-injected callbacks for the actual navigation.

## Steps

### 1. Confirm the destination route exists in `screen-api`

In `:ui-screen-features:screen-api/RootRouter.kt`:

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data object Authorization : RootRouter()
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
    @Serializable public data class Training(val stage: StageState) : RootRouter()
}

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable public data object Body : ProfileRouter()
    @Serializable public data object Settings : ProfileRouter()
    @Serializable public data class WorkoutHistory(val initialRange: DateRange) : ProfileRouter()
}
```

If the destination route doesn't exist, add it here first.

### 2. Add a `RootDirection` entry

In `:shared`'s `RootDirection.kt` (note: `RootDirection` is `public sealed interface` — `:shared` is the composition root and re-exports it):

```kotlin
public sealed interface RootDirection : BaseDirection {
    public data object Login : RootDirection
    public data object Home : RootDirection
    public data object Profile : RootDirection
    // ... existing entries: WeightHistory, MissingEquipment, ExcludedMuscles,
    //     Experience, Settings, Social, Goal, Debug, Trainings, Back, Close, ...
    public data class OpenProfileWorkoutHistory(val initialRange: DateRange) : RootDirection
}
```

Or, a more general route-based approach:

```kotlin
public data class GoTo(val target: RootRouter) : RootDirection
```

The reference repo uses the explicit-direction style — most existing entries are `data object` (e.g. `WeightHistory`, `Settings`) that map 1-to-1 to a profile sub-route, with `data class` reserved for entries that carry a payload (e.g. `Training(val stage: StageState)`).

### 3. Expose the callback on `RootContract` (or `RootViewModel`)

`RootContract.kt` is `public` (the contract crosses the module boundary because `RootScreen` and any other consumer in `:shared` reads it):

```kotlin
@Immutable
public interface RootContract {
    // ... existing
    public fun toProfileWorkoutHistory(range: DateRange)

    @Immutable
    public companion object Empty : RootContract {
        // ...
        override fun toProfileWorkoutHistory(range: DateRange) = Unit
    }
}
```

`RootViewModel.kt`:

```kotlin
internal class RootViewModel(...) : BaseViewModel<...>(...), RootContract {

    override fun toProfileWorkoutHistory(range: DateRange) {
        navigateTo(RootDirection.OpenProfileWorkoutHistory(range))
    }
}
```

### 4. Translate the `Direction` in `RootComponent.eventListener`

```kotlin
override suspend fun eventListener(direction: RootDirection) {
    when (direction) {
        RootDirection.Login -> navigation.replaceAll(RootRouter.Auth(AuthRouter.AuthProcess))
        RootDirection.Home -> navigation.replaceAll(RootRouter.Home)
        RootDirection.Profile -> navigation.push(RootRouter.Profile(ProfileRouter.Body))
        is RootDirection.OpenProfileWorkoutHistory -> navigation.push(
            RootRouter.Profile(ProfileRouter.WorkoutHistory(direction.initialRange))
        )
        // ... existing branches
    }
}
```

The real `RootRouter.Auth` wraps an `AuthRouter` value; `RootRouter.Profile(ProfileRouter.<Sub>)` is the way every profile sub-screen is opened.

### 5. Thread the callback through `:ui-screen-features:home`

When `RootComponent.createChild` creates the `HomeRootComponent`, pass the callback:

```kotlin
private fun createChild(config: RootRouter, ctx: ComponentContext): Child = when (config) {
    is RootRouter.Home -> Child.Home(
        HomeRootComponent(
            componentContext = ctx,
            toProfileWorkoutHistory = viewModel::toProfileWorkoutHistory,
            toProfile = viewModel::toProfile,
            // ... other callbacks
        )
    )
    // ...
}
```

`HomeRootComponent` is a `public class` (it's instantiated by `RootComponent.createChild` in `:shared`, across the module boundary). Its `childStack` uses `initialStack = { listOf(initial) }` + `handleBackButton = true`:

```kotlin
public class HomeRootComponent(
    initial: HomeRouter,
    componentContext: ComponentContext,
    private val toProfileWorkoutHistory: (DateRange) -> Unit,
    // ... other constructor callbacks
) : BaseComponent<HomeRootDirection>(componentContext) {

    private val navigation = StackNavigation<HomeRouter>()

    internal val childStack: Value<ChildStack<HomeRouter, Child>> = childStack(
        source = navigation,
        serializer = HomeRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "HomeRootComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: HomeRouter, context: ComponentContext): Child = when (router) {
        is HomeRouter.Home -> Child.Home(
            HomeComponent(
                componentContext = context,
                toProfileWorkoutHistory = toProfileWorkoutHistory,
                back = viewModel::onBack,
            )
        )
    }
}
```

Each child component takes a `toProfileWorkoutHistory: (DateRange) -> Unit` lambda in its constructor. The `back` lambda emits `<Feature>RootDirection.Back`, which `eventListener` maps to `close.invoke()` (the lambda received from `RootComponent.createChild`).

### 6. Use the callback in the originating ViewModel

```kotlin
internal class HomeOverviewViewModel(
    // ...
) : BaseViewModel<HomeOverviewState, HomeOverviewDirection, HomeOverviewLoader>(...), HomeOverviewContract {

    override fun onChartClick(range: DateRange) {
        navigateTo(HomeOverviewDirection.OpenWorkoutHistory(range))
    }
}
```

`HomeOverviewComponent`:

```kotlin
internal class HomeOverviewComponent(
    componentContext: ComponentContext,
    private val toProfileWorkoutHistory: (DateRange) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<HomeOverviewDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance { HomeOverviewViewModel(/* … */) }

    override suspend fun eventListener(direction: HomeOverviewDirection) {
        when (direction) {
            HomeOverviewDirection.Back -> back.invoke()
            is HomeOverviewDirection.OpenWorkoutHistory -> toProfileWorkoutHistory(direction.range)
        }
    }
    // ...
}
```

## Visual flow

```
HomeOverviewViewModel.onChartClick(range)
  → navigateTo(HomeOverviewDirection.OpenWorkoutHistory(range))
    → HomeOverviewComponent.eventListener(direction) → toProfileWorkoutHistory(range)
      → (lambda) RootViewModel.toProfileWorkoutHistory(range)
        → navigateTo(RootDirection.OpenProfileWorkoutHistory(range))
          → RootComponent.eventListener(direction) → navigation.push(RootRouter.Profile(ProfileRouter.WorkoutHistory(range)))
            → ProfileRootComponent.createChild — opens the WorkoutHistory screen
```

Verbose? Yes. But every step is **explicit** — no shared global event bus, no service locator.

## Alternative: result-back navigation

If feature A opens feature B and expects a result back, see `03-architecture-patterns/04-cross-component-results.md` for the `ResultManager` pattern.

## Common mistakes

- **Direct feature-to-feature import.** Forbidden. Use `screen-api` + callbacks.
- **Lambda parameter in a `Router` payload.** Routes are `@Serializable`; lambdas aren't.
- **Skipping the `RootDirection` step.** Trying to call `RootRouter.push(...)` directly from a feature VM has no path.
- **Forgetting to thread the callback** through every layer (`createChild` → child component constructor → child VM). Compile errors point to the missing step.
- **Adding a `toX(...)` method on `<Feature>Contract`** when it could be a callback. Use a constructor lambda instead — `Contract` is for the UI's own callbacks, not cross-feature routing.

## When to add a new route

When the destination route doesn't exist in `screen-api`. Add it to the right `<Feature>Router` (or the top-level `RootRouter` if it's a truly new top-level feature). Update `createChild` in the destination feature's root component to handle the new route.

When the destination route exists but takes different parameters, **add a new subtype** rather than mutating the existing one. Backwards compatibility avoids breaking other callers.
