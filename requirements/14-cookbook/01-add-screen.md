# Add a Screen

How to add a new sub-screen inside an existing feature module — e.g. a "Note archive" screen in `:ui-screen-features:profile`.

## Steps

### 1. Create the package

```
ui-screen-features/profile/src/commonMain/kotlin/com/<org>/<product>/profile/notearchive/
```

### 2. Create the seven files

`ProfileNoteArchiveState.kt`:

```kotlin
package com.<org>.<product>.profile.notearchive

@Immutable
internal data class ProfileNoteArchiveState(
    val range: DateRangeFormatState = DateRangeFormatState.of(DateRangePresets.last30Days()),
    val items: ImmutableList<NoteRowState> = persistentListOf(),
)
```

State is not `@Serializable` — the Decompose `StateKeeper` serializes Routers, not feature state. Provide defaults directly in the constructor (`BaseViewModel(ProfileNoteArchiveState())`); do not declare a `companion object Empty` on State (Empty belongs on `Contract`, not `State`).

`ProfileNoteArchiveDirection.kt`:

```kotlin
internal sealed interface ProfileNoteArchiveDirection : BaseDirection {
    data object Back : ProfileNoteArchiveDirection
    data class OpenNote(val id: String) : ProfileNoteArchiveDirection
}
```

`ProfileNoteArchiveLoader.kt`:

```kotlin
@Immutable
internal sealed interface ProfileNoteArchiveLoader : BaseLoader {
    @Immutable data object LoadHistory : ProfileNoteArchiveLoader
}
```

`ProfileNoteArchiveContract.kt`:

```kotlin
@Immutable
internal interface ProfileNoteArchiveContract {
    fun onBack()
    fun onRangeChange(range: DateRange)
    fun onNoteClick(id: String)

    companion object Empty : ProfileNoteArchiveContract {
        override fun onBack() = Unit
        override fun onRangeChange(range: DateRange) = Unit
        override fun onNoteClick(id: String) = Unit
    }
}
```

`ProfileNoteArchiveViewModel.kt`:

```kotlin
internal class ProfileNoteArchiveViewModel(
    initialRange: DateRange,
    private val noteFeature: NoteFeature,
) : BaseViewModel<ProfileNoteArchiveState, ProfileNoteArchiveDirection, ProfileNoteArchiveLoader>(
    ProfileNoteArchiveState(range = DateRangeFormatState.of(initialRange)),
), ProfileNoteArchiveContract {

    init {
        noteFeature.observeNotes(initialRange.from, initialRange.to)
            .map { it.toState() }
            .onEach { items -> update { it.copy(items = items) } }
            .safeLaunch()

        safeLaunch(loader = ProfileNoteArchiveLoader.LoadHistory) {
            noteFeature.getNotes(initialRange.from, initialRange.to).getOrThrow()
        }
    }

    override fun onBack() {
        navigateTo(ProfileNoteArchiveDirection.Back)
    }

    override fun onRangeChange(range: DateRange) {
        update { it.copy(range = DateRangeFormatState.of(range)) }
        safeLaunch(loader = ProfileNoteArchiveLoader.LoadHistory) {
            noteFeature.getNotes(range.from, range.to).getOrThrow()
        }
    }

    override fun onNoteClick(id: String) {
        navigateTo(ProfileNoteArchiveDirection.OpenNote(id))
    }
}
```

`ProfileNoteArchiveComponent.kt`:

```kotlin
internal class ProfileNoteArchiveComponent(
    componentContext: ComponentContext,
    initialRange: DateRange,
    private val back: () -> Unit,
    private val toNote: (String) -> Unit,
) : BaseComponent<ProfileNoteArchiveDirection>(componentContext) {

    override val viewModel: ProfileNoteArchiveViewModel = componentContext.retainedInstance {
        ProfileNoteArchiveViewModel(
            initialRange = initialRange,
            noteFeature = getKoin().get(),
        )
    }

    override suspend fun eventListener(direction: ProfileNoteArchiveDirection) {
        when (direction) {
            ProfileNoteArchiveDirection.Back -> back()
            is ProfileNoteArchiveDirection.OpenNote -> toNote(direction.id)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileNoteArchiveScreen(state.value, loaders.value, viewModel)
    }
}
```

`ProfileNoteArchiveScreen.kt`:

```kotlin
@Composable
internal fun ProfileNoteArchiveScreen(
    state: ProfileNoteArchiveState,
    loaders: ImmutableSet<ProfileNoteArchiveLoader>,
    contract: ProfileNoteArchiveContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.profile_note_archive_title),
        leading = Leading.Back(onClick = contract::onBack),
    )

    // ... content (LazyColumn of items, range picker, etc.)
}

@AppPreview
@Composable
private fun ProfileNoteArchiveScreenPreview() {
    PreviewContainer {
        ProfileNoteArchiveScreen(
            state = ProfileNoteArchiveState(items = stubNoteList()),
            loaders = persistentSetOf(),
            contract = ProfileNoteArchiveContract.Empty,
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
    @Serializable public data class NoteArchive(val initialRange: DateRange) : ProfileRouter()
}
```

### 4. Wire the route in `ProfileComponent.createChild`

`ProfileComponent` owns the inner `StackNavigation<ProfileRouter>` (built with `initialStack = { listOf(initial) }`, `handleBackButton = true`, `key = "ProfileComponent"`). Add the new branch in `createChild` and a matching subtype on the `internal sealed class Child`:

```kotlin
private fun createChild(router: ProfileRouter, context: ComponentContext): Child = when (router) {
    ProfileRouter.Overview -> Child.ProfileOverview(
        ProfileOverviewComponent(componentContext = context, back = viewModel::onBack)
    )
    ProfileRouter.Settings -> Child.Settings(
        ProfileSettingsComponent(componentContext = context, back = viewModel::onBack)
    )
    is ProfileRouter.NoteArchive -> Child.NoteArchive(
        ProfileNoteArchiveComponent(
            componentContext = context,
            initialRange = router.initialRange,
            back = viewModel::onBack,
            toNote = { id -> /* navigate to note details */ },
        )
    )
    // ... other existing branches
}

internal sealed class Child(open val component: BaseComponent<*>) {
    data class ProfileOverview(override val component: ProfileOverviewComponent) : Child(component)
    data class Settings(override val component: ProfileSettingsComponent) : Child(component)
    data class NoteArchive(override val component: ProfileNoteArchiveComponent) : Child(component)
    // ... other existing subtypes
}
```

`viewModel::onBack` emits `ProfileDirection.Back`, which the parent (`RootComponent`) translates into `navigation.pop()` via `close = viewModel::onBack`. Don't call `navigation.pop()` directly from `createChild`.

### 5. Update the calling screen to trigger navigation

In `ProfileOverviewViewModel`:

```kotlin
override fun onNoteArchiveClick() {
    navigateTo(ProfileOverviewDirection.OpenNoteArchive(state.value.defaultRange))
}
```

In `ProfileOverviewComponent`:

```kotlin
override suspend fun eventListener(direction: ProfileOverviewDirection) {
    when (direction) {
        is ProfileOverviewDirection.OpenNoteArchive -> toNoteArchive(direction.initialRange)
        // ...
    }
}
```

`toNoteArchive: (DateRange) -> Unit` is passed via the Component constructor from `ProfileComponent.createChild` (see step 4). The reference repo's profile feature root is `ProfileComponent` (bare feature name), not `ProfileRootComponent` — only features whose root and primary sub-screen share a name use the `*RootComponent` suffix.

### 6. Verify

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both should build green. Visually verify the new screen in a debug run.

## What you did NOT have to do

- **No new Koin module.** `NoteFeature` is already provided by an existing data feature module.
- **No new mapper module.** `.toState()` extension goes in `:data-mappers:domain-to-state/notearchive/`.
- **No changes to `:shared/Koin.kt`.** No new module to register.
- **No changes to `RootRouter`.** New route is in `ProfileRouter`, not at the root level.

## Common mistakes

- **Forgetting `@Serializable` on the new `Router` subtype.** Crashes on process death.
- **Forgetting `key = "ProfileComponent"` on `childStack`.** Already in place (from the existing feature), but a new feature module sometimes omits it.
- **Including the new screen as a top-level RootRouter entry.** Sub-screens belong inside `<Feature>Router`, not at the root.
- **Threading `NoteFeature` through Component constructors.** Inject via `getKoin().get()` inside `retainedInstance`.
- **Passing a `() -> Unit` callback in `*Router`** instead of via Component constructor. Routes are `@Serializable` data; callbacks aren't.
