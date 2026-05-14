# Add a Screen

How to add a new sub-screen inside an existing feature module — e.g. a "Workout history" screen in `:ui-screen-features:profile`.

## Steps

### 1. Create the package

```
ui-screen-features/profile/src/commonMain/kotlin/com/<org>/<product>/profile/workouthistory/
```

### 2. Create the seven files

`ProfileWorkoutHistoryState.kt`:

```kotlin
package com.<org>.<product>.profile.workouthistory

@Immutable
internal data class ProfileWorkoutHistoryState(
    val range: DateRangeFormatState = DateRangeFormatState.of(DateRangePresets.last30Days()),
    val items: ImmutableList<WorkoutHistoryRowState> = persistentListOf(),
)
```

State is not `@Serializable` — the Decompose `StateKeeper` serializes Routers, not feature state. Provide defaults directly in the constructor (`BaseViewModel(ProfileWorkoutHistoryState())`); do not declare a `companion object Empty` on State (Empty belongs on `Contract`, not `State`).

`ProfileWorkoutHistoryDirection.kt`:

```kotlin
internal sealed interface ProfileWorkoutHistoryDirection : BaseDirection {
    data object Back : ProfileWorkoutHistoryDirection
    data class OpenWorkout(val id: String) : ProfileWorkoutHistoryDirection
}
```

`ProfileWorkoutHistoryLoader.kt`:

```kotlin
@Immutable
internal sealed interface ProfileWorkoutHistoryLoader : BaseLoader {
    @Immutable data object LoadHistory : ProfileWorkoutHistoryLoader
}
```

`ProfileWorkoutHistoryContract.kt`:

```kotlin
@Immutable
internal interface ProfileWorkoutHistoryContract {
    fun onBack()
    fun onRangeChange(range: DateRange)
    fun onWorkoutClick(id: String)

    companion object Empty : ProfileWorkoutHistoryContract {
        override fun onBack() = Unit
        override fun onRangeChange(range: DateRange) = Unit
        override fun onWorkoutClick(id: String) = Unit
    }
}
```

`ProfileWorkoutHistoryViewModel.kt`:

```kotlin
internal class ProfileWorkoutHistoryViewModel(
    initialRange: DateRange,
    private val workoutHistoryFeature: WorkoutHistoryFeature,
) : BaseViewModel<ProfileWorkoutHistoryState, ProfileWorkoutHistoryDirection, ProfileWorkoutHistoryLoader>(
    ProfileWorkoutHistoryState(range = DateRangeFormatState.of(initialRange)),
), ProfileWorkoutHistoryContract {

    init {
        workoutHistoryFeature.observeWorkoutHistory(initialRange.from, initialRange.to)
            .map { it.toState() }
            .onEach { items -> update { it.copy(items = items) } }
            .safeLaunch()

        safeLaunch(loader = ProfileWorkoutHistoryLoader.LoadHistory) {
            workoutHistoryFeature.getWorkoutHistory(initialRange.from, initialRange.to).getOrThrow()
        }
    }

    override fun onBack() {
        navigateTo(ProfileWorkoutHistoryDirection.Back)
    }

    override fun onRangeChange(range: DateRange) {
        update { it.copy(range = DateRangeFormatState.of(range)) }
        safeLaunch(loader = ProfileWorkoutHistoryLoader.LoadHistory) {
            workoutHistoryFeature.getWorkoutHistory(range.from, range.to).getOrThrow()
        }
    }

    override fun onWorkoutClick(id: String) {
        navigateTo(ProfileWorkoutHistoryDirection.OpenWorkout(id))
    }
}
```

`ProfileWorkoutHistoryComponent.kt`:

```kotlin
internal class ProfileWorkoutHistoryComponent(
    componentContext: ComponentContext,
    initialRange: DateRange,
    private val back: () -> Unit,
    private val toWorkout: (String) -> Unit,
) : BaseComponent<ProfileWorkoutHistoryDirection>(componentContext) {

    override val viewModel: ProfileWorkoutHistoryViewModel = componentContext.retainedInstance {
        ProfileWorkoutHistoryViewModel(
            initialRange = initialRange,
            workoutHistoryFeature = getKoin().get(),
        )
    }

    override suspend fun eventListener(direction: ProfileWorkoutHistoryDirection) {
        when (direction) {
            ProfileWorkoutHistoryDirection.Back -> back()
            is ProfileWorkoutHistoryDirection.OpenWorkout -> toWorkout(direction.id)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileWorkoutHistoryScreen(state.value, loaders.value, viewModel)
    }
}
```

`ProfileWorkoutHistoryScreen.kt`:

```kotlin
@Composable
internal fun ProfileWorkoutHistoryScreen(
    state: ProfileWorkoutHistoryState,
    loaders: ImmutableSet<ProfileWorkoutHistoryLoader>,
    contract: ProfileWorkoutHistoryContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.profile_workout_history_title),
        leading = Leading.Back(onClick = contract::onBack),
    )

    // ... content (LazyColumn of items, range picker, etc.)
}

@AppPreview
@Composable
private fun ProfileWorkoutHistoryScreenPreview() {
    PreviewContainer {
        ProfileWorkoutHistoryScreen(
            state = ProfileWorkoutHistoryState(items = stubWorkoutHistoryRowList()),
            loaders = persistentSetOf(),
            contract = ProfileWorkoutHistoryContract.Empty,
        )
    }
}
```

### 3. Add the route to `:ui-screen-features:screen-api`

In `ProfileRouter.kt`:

```kotlin
@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable public data object Body : ProfileRouter()
    @Serializable public data object Settings : ProfileRouter()
    @Serializable public data class WorkoutHistory(val initialRange: DateRange) : ProfileRouter()
}
```

### 4. Wire the route in `ProfileComponent.createChild`

`ProfileComponent` owns the inner `StackNavigation<ProfileRouter>` (built with `initialStack = { listOf(initial) }`, `handleBackButton = true`, `key = "ProfileComponent"`). Add the new branch in `createChild` and a matching subtype on the `internal sealed class Child`:

```kotlin
private fun createChild(router: ProfileRouter, context: ComponentContext): Child = when (router) {
    ProfileRouter.Body -> Child.ProfileBody(
        ProfileBodyComponent(componentContext = context, back = viewModel::onBack)
    )
    ProfileRouter.Settings -> Child.Settings(
        ProfileSettingsComponent(componentContext = context, back = viewModel::onBack)
    )
    is ProfileRouter.WorkoutHistory -> Child.WorkoutHistory(
        ProfileWorkoutHistoryComponent(
            componentContext = context,
            initialRange = router.initialRange,
            back = viewModel::onBack,
            toWorkout = { id -> /* navigate to workout details */ },
        )
    )
    // ... other existing branches
}

internal sealed class Child(open val component: BaseComponent<*>) {
    data class ProfileBody(override val component: ProfileBodyComponent) : Child(component)
    data class Settings(override val component: ProfileSettingsComponent) : Child(component)
    data class WorkoutHistory(override val component: ProfileWorkoutHistoryComponent) : Child(component)
    // ... other existing subtypes
}
```

`viewModel::onBack` emits `ProfileDirection.Back`, which the parent (`RootComponent`) translates into `navigation.pop()` via `close = viewModel::onBack`. Don't call `navigation.pop()` directly from `createChild`.

### 5. Update the calling screen to trigger navigation

In `ProfileBodyViewModel`:

```kotlin
override fun onWorkoutHistoryClick() {
    navigateTo(ProfileBodyDirection.OpenWorkoutHistory(state.value.defaultRange))
}
```

In `ProfileBodyComponent`:

```kotlin
override suspend fun eventListener(direction: ProfileBodyDirection) {
    when (direction) {
        is ProfileBodyDirection.OpenWorkoutHistory -> toWorkoutHistory(direction.initialRange)
        // ...
    }
}
```

`toWorkoutHistory: (DateRange) -> Unit` is passed via the Component constructor from `ProfileRootComponent.createChild` (see step 4).

### 6. Verify

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both should build green. Visually verify the new screen in a debug run.

## What you did NOT have to do

- **No new Koin module.** `WorkoutHistoryFeature` is already provided by an existing data feature module.
- **No new mapper module.** `.toState()` extension goes in `:data-mappers:domain-to-state/workouthistory/`.
- **No changes to `:shared/Koin.kt`.** No new module to register.
- **No changes to `RootRouter`.** New route is in `ProfileRouter`, not at the root level.

## Common mistakes

- **Forgetting `@Serializable` on the new `Router` subtype.** Crashes on process death.
- **Forgetting `key = "ProfileRootComponent"` on `childStack`.** Already in place (from the existing feature), but a new feature module sometimes omits it.
- **Including the new screen as a top-level RootRouter entry.** Sub-screens belong inside `<Feature>Router`, not at the root.
- **Threading `WorkoutHistoryFeature` through Component constructors.** Inject via `getKoin().get()` inside `retainedInstance`.
- **Passing a `() -> Unit` callback in `*Router`** instead of via Component constructor. Routes are `@Serializable` data; callbacks aren't.
