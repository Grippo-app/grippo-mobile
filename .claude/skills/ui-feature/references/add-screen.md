# Recipe — Add a Screen (sub-screen inside an existing feature)

> Read alongside: `references/mvi-contract.md` (seven-file contract), `references/base-classes.md` (BaseViewModel/Component/Screen), `references/compose-rules.md` (§ Entity composables).

> **Concrete example.** Identifiers below (`Note archive`, etc.) are illustrative; the steps apply to any feature.

> **Analytics cross-cut:** apply [feature-analytics](../../feature-analytics/SKILL.md).
> Examples below show MVI/navigation only. Every independent screen must declare screenPresentation
> in its VM; its Component resolves analytics as a normal retained-VM dependency.
> Reuse the feature API/implementation and update actual coverage; add no provider.

> **State preservation:** apply [state-preservation](../../state-preservation/SKILL.md).
> Classify user-owned drafts and current filters before implementation; verify a
> fresh-component archive roundtrip. Route inputs seed state; restored drafts win.
> Apply the [draft policy](../../state-preservation/SKILL.md#data-backed-form-synchronization):
> no runtime flags/source caches; fresh forms populate once. This recipe's live
> items observer rebuilds display data and must not reinitialize editable inputs.


> **Design fidelity:** read the actual supplied Figma node or design artifact before implementation. Match its content, layout, tokens and declared states; inspect rendered previews when permitted and record unverified states.

> **Figma-backed visual review:** seed the normal `@AppPreview` from the design’s actual text, values and sections for each affected state. Compare a permitted rendered preview or app screenshot with its declared design node; record observations and unavailable runtime checks. Follow [visual fidelity review](../../validation-gates/references/visual-review.md). Add no runner, dependency or recording source set.

How to add a new sub-screen inside an existing feature module — e.g. a "Note archive" screen in `:ui-screen-features:profile`.

## 1. Create the package
```
ui-screen-features/profile/src/commonMain/kotlin/com/grippo/profile/notearchive/
```

## 2. Create the seven files

`ProfileNoteArchiveState.kt` — preserve the user-selected range; rebuild items from
data Flows. Use the module's serialization plugin and the existing
[Component business state preservation](process-death.md#editable-form-drafts).
**No `companion object Empty` on State** (Empty belongs on Contract):
```kotlin
@Immutable
@Serializable
internal data class ProfileNoteArchiveState(
    val range: DateRangeFormatState = DateRangeFormatState.Empty(),
    @Transient
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
    private val noteFeature: NoteFeature,
    initialState: ProfileNoteArchiveState = ProfileNoteArchiveState(),
) : BaseViewModel<ProfileNoteArchiveState, ProfileNoteArchiveDirection, ProfileNoteArchiveLoader>(
    initialState,
), ProfileNoteArchiveContract {
    init {
        state.map { it.range.value }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { range -> noteFeature.observeNotes(range.from, range.to) }
            .map { it.toState() }
            .onEach { items -> update { it.copy(items = items) } }
            .safeLaunch()
        safeLaunch(loader = ProfileNoteArchiveLoader.LoadHistory) {
            val range = state.value.range.value ?: return@safeLaunch
            noteFeature.getNotes(range.from, range.to).getOrThrow()
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
import com.grippo.core.foundation.models.PersistenceKey

internal class ProfileNoteArchiveComponent(
    componentContext: ComponentContext,
    initialRange: DateRange,
    private val back: () -> Unit,
    private val toNote: (String) -> Unit,
) : BaseComponent<ProfileNoteArchiveDirection>(componentContext) {
    override val viewModel: ProfileNoteArchiveViewModel = componentContext.retainedInstance {
        ProfileNoteArchiveViewModel(
            noteFeature = getKoin().get(),
            initialState = restoreState(DRAFT)
                ?: ProfileNoteArchiveState(range = DateRangeFormatState.of(initialRange)),
        )
    }
    init {
        saveState(DRAFT) { viewModel.state.value }
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

    private companion object {
        val DRAFT = PersistenceKey("NoteArchiveDraft", ProfileNoteArchiveState.serializer())
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

With a supplied design, preview its actual representative content and declared states.

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
Reconcile leaf/control/input/operation coverage and run the affected checks from
the analytics skill within the user's verification scope. Record explicit skips.
Run its [mandatory screen coverage gate](../../feature-analytics/SKILL.md#mandatory-screen-coverage-gate):
verify all new/affected screens have their VM hook and all containers keep null.
Missing screen hooks block completion.
Apply the [mandatory preservation gate](../../state-preservation/SKILL.md#mandatory-preservation-gate)
to current range/drafts and rebuilt items; confirm restored values, Back and secret
exclusion in the user's permitted verification scope.
```bash
# Current project build tasks;
# Run only when the user's verification scope permits the relevant platform.
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```
When permitted, both must build green. Visually verify in a permitted debug run.

## What you did NOT have to do
- No new Koin module (`NoteFeature` is already provided).
- No new mapper module (`.toState()` goes in `:data-mappers:domain-to-state/note/`).
- No `:shared/Koin.kt` change.
- No `RootRouter` change (the new route is in `ProfileRouter`, not at root level).

These statements describe the MVI example with existing dependencies. A missing
feature analytics implementation still needs its normal module/shared registration
under the canonical skill; never create a UI integration module to replace it.

## Common mistakes (MUST avoid)
- **Forgetting `@Serializable` on the new `Router` subtype.** Crashes on process death.
- **Forgetting `key = "ProfileComponent"` on `childStack`** (a new feature module sometimes omits it).
- **Including the new screen as a top-level `RootRouter` entry.** Sub-screens belong inside `<Feature>Router`.
- **Threading `NoteFeature` through Component constructors.** Inject via `getKoin().get()` inside `retainedInstance`.
- **Passing a `() -> Unit` callback in `*Router`** instead of via the Component constructor. Routes are `@Serializable` data; callbacks aren't.
