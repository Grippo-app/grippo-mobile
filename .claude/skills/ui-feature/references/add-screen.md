# Recipe — Add a Screen (sub-screen inside an existing feature)

> Read alongside: `references/mvi-contract.md` (seven-file contract), `references/base-classes.md` (BaseViewModel/Component/Screen), `references/compose-rules.md` (§ Entity composables).

> **Concrete example.** Identifiers below (`Note archive`, etc.) are illustrative; the steps apply to any feature.

> **Figma-enabled (`figmaEnabled: true`) + a `## Design` bullet:** the builder's design-cache gate applies BEFORE any file from this recipe is written — read the cached spec/mock and stop on unmapped components (see SKILL.md "Stop and ask"). This recipe owns the code shape; that gate owns when you may start.

> **Build to the pulled design — not your own interpretation (mandatory for a `figmaEnabled` screen).** The pull session already wrote this screen's design to the cache — you build to THAT, you do not invent a layout. For each `## Design` bullet, read the pulled value spec `orchestrator/.cache/figma/screens/<stem>/<Screen>.spec.json` (it drives the element inventory, layout, paddings/gaps, corner radii, fills/tokens, and text styles — build every one to match) AND look at the oracle mock `orchestrator/.cache/figma/screens/<stem>/<Screen>.png` (it drives the overall appearance and which content/sections actually exist — reproduce the design's structure and its representative content). Do NOT build a different, simpler, or made-up screen. **Inventing a different layout or fabricating screen content is the ROOT cause of a failed screenshot comparison:** the gate judges the built screen against THIS design, so build to THIS design. Per dark theme, consult the dark spec/oracle (`<Screen>.dark.spec.json` / `<Screen>.dark.png`) too. (Golden invariant: you read these PULLED cache files — you never call Figma/MCP yourself; only the pull session does.)

> **Screenshot-fidelity gate (mandatory for a `figmaEnabled` screen):** add `id("screenshot.test.convention")` to the module's `build.gradle.kts` plugins and create `ScreenshotTest.kt` under `src/androidHostTest/` — capture by construction. The gate is mandatory for a declared `## Design` screen and a missing capture is a BLOCKER (only a non-UI task self-skips). **Seed the `@AppPreview` stub + each ScreenshotTest state from the design's ACTUAL content shown in the oracle** — the real text/values/sections visible in `<Screen>.png`, per declared state — NOT generic invented data (do not stub `HomeNetwork_5G / 312 Mbps` when the design shows different content). One capture per declared state (see the gate spec's §2.1 multi-state default and §2 content-parity rule — do not duplicate them here). A stub whose content diverges from the oracle is a legitimate BLOCKER you fix by matching the design, never by relaxing the gate.

How to add a new sub-screen inside an existing feature module — e.g. a "Note archive" screen in `:ui-screen-features:profile`.

## 1. Create the package
```
ui-screen-features/profile/src/commonMain/kotlin/com/<org>/<product>/profile/notearchive/
```

## 2. Create the seven files

`ProfileNoteArchiveState.kt` — defaults in ctor; State is **not** `@Serializable` (the StateKeeper serializes Routers, not feature state); **no `companion object Empty` on State** (Empty belongs on Contract):
```kotlin
@Immutable
internal data class ProfileNoteArchiveState(
    val range: DateRangeFormatState = DateRangeFormatState.of(DateRangePresets.last30Days()),
    val items: PersistentList<NoteState> = persistentListOf(),
)
```

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

`ProfileNoteArchiveContract.kt` — `@Immutable interface` with `onBack`/`onRangeChange`/`onNoteClick` + `companion object Empty` of no-ops.

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
    override fun onBack() { navigateTo(ProfileNoteArchiveDirection.Back) }
    override fun onRangeChange(range: DateRange) {
        update { it.copy(range = DateRangeFormatState.of(range)) }
        safeLaunch(loader = ProfileNoteArchiveLoader.LoadHistory) {
            noteFeature.getNotes(range.from, range.to).getOrThrow()
        }
    }
    override fun onNoteClick(id: String) { navigateTo(ProfileNoteArchiveDirection.OpenNote(id)) }
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
        ProfileNoteArchiveViewModel(initialRange = initialRange, noteFeature = getKoin().get())
    }
    override suspend fun eventListener(direction: ProfileNoteArchiveDirection) {
        when (direction) {
            ProfileNoteArchiveDirection.Back -> back()
            is ProfileNoteArchiveDirection.OpenNote -> toNote(direction.id)
        }
    }
    @Composable override fun Render() {
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
    LazyColumn {
        items(state.items, key = { it.id }, contentType = { "NoteRow" }) { note ->
            NoteRow(state = note, modifier = Modifier.animateItem(), onClick = { contract.onNoteClick(note.id) })
        }
    }
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

The list renders each entity through `NoteRow(state = note)` — one parameter, never fields spread flat. `NoteRowState` and its `stubNoteList()` factory live in `:ui-core:state`. Screen-specific sub-composables (a custom header, stats card, empty-state block) go in a `notearchive/components/` subfolder, one cohesive composable per file, with private helpers + `@AppPreview` co-located.

For a `figmaEnabled` screen the `@AppPreview` stub (`stubNoteList()` here) is not free-form filler — it must carry the design's representative content, because the ScreenshotTest copies this stub construction as its capture seed (gate spec §2). Fill it from the oracle `<Screen>.png` / the pulled `<Screen>.spec.json` frame content — the actual sections/text/values the design shows — so the built preview renders the design, not made-up data.

## 3. Add the route to `:ui-screen-features:screen-api`
In `ProfileRouter.kt`:
```kotlin
@Serializable public data class NoteArchive(val initialRange: DateRange) : ProfileRouter()
```

## 4. Wire the route in `ProfileComponent.createChild`
`ProfileComponent` owns the inner `StackNavigation<ProfileRouter>` (`initialStack = { listOf(initial) }`, `handleBackButton = true`, `key = "ProfileComponent"`). Add a `createChild` branch + a matching `Child` subtype:
```kotlin
is ProfileRouter.NoteArchive -> Child.NoteArchive(
    ProfileNoteArchiveComponent(
        componentContext = context,
        initialRange = router.initialRange,
        back = viewModel::onBack,
        toNote = { id -> /* navigate to note details */ },
    )
)
// internal sealed class Child(...): data class NoteArchive(override val component: ProfileNoteArchiveComponent) : Child(component)
```
`viewModel::onBack` emits `ProfileDirection.Back`, which the parent translates into `navigation.pop()`. Don't call `navigation.pop()` directly from `createChild`.

## 5. Update the calling screen to trigger navigation
`ProfileOverviewViewModel`: `override fun onNoteArchiveClick() { navigateTo(ProfileOverviewDirection.OpenNoteArchive(state.value.defaultRange)) }`.
`ProfileOverviewComponent.eventListener`: `is ProfileOverviewDirection.OpenNoteArchive -> toNoteArchive(direction.initialRange)`. `toNoteArchive: (DateRange) -> Unit` is passed via the Component ctor from `ProfileComponent.createChild`.

## 6. Verify
```bash
# Task names come from project-config (sharedFrameworkTask / androidAssembleTask) —
# these are the template defaults; skip the framework task when iosEnabled: false.
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```
Both must build green. Visually verify the new screen in a debug run.

## What you did NOT have to do
- No new Koin module (`NoteFeature` is already provided).
- No new mapper module (`.toState()` goes in `:data-mappers:domain-to-state/note/`).
- No `:shared/Koin.kt` change.
- No `RootRouter` change (the new route is in `ProfileRouter`, not at root level).

## Common mistakes (MUST avoid)
- **Forgetting `@Serializable` on the new `Router` subtype.** Crashes on process death.
- **Forgetting `key = "ProfileComponent"` on `childStack`** (a new feature module sometimes omits it).
- **Including the new screen as a top-level `RootRouter` entry.** Sub-screens belong inside `<Feature>Router`.
- **Threading `NoteFeature` through Component constructors.** Inject via `getKoin().get()` inside `retainedInstance`.
- **Passing a `() -> Unit` callback in `*Router`** instead of via the Component constructor. Routes are `@Serializable` data; callbacks aren't.
