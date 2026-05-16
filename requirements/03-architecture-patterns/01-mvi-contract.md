# MVI Contract

Every screen and dialog in the project follows the same MVI template: **seven files** per screen/dialog, same names, same shape, same flow of data.

## The seven files

```
<feature>/
  <Name>Component.kt       // Decompose Component; owns ViewModel; handles Direction
  <Name>Contract.kt        // @Immutable interface with on*-callbacks + companion object Empty
  <Name>State.kt           // @Immutable data class | data object | sealed interface
  <Name>Direction.kt       // sealed interface : BaseDirection (navigation intents)
  <Name>Loader.kt          // @Immutable sealed interface : BaseLoader (active operations)
  <Name>ViewModel.kt       // BaseViewModel<State, Direction, Loader>, implements Contract
  <Name>Screen.kt          // @Composable internal fun (state, loaders, contract)
```

`<Name>` is the screen/dialog name (e.g. `NoteEditor`, `AmountPicker`). Files are siblings in the same package.

## Flow of data

```
UI tap
  → Screen invokes contract.onApplyClick()
    → ViewModel runs domain logic (safeLaunch, withLoader, calls Feature)
      → ViewModel.update { it.copy(...) }  →  new State emitted on viewModel.state StateFlow
        → Screen recomposes
  ⟶ on completion:
    → ViewModel.navigateTo(Direction.Close)  →  navigator: Flow<DIRECTION>
      → Component.eventListener handles the Direction (back() / navigation.push(...))
```

Loaders run in parallel:

```
ViewModel.withLoader(MyLoader.LoadingData) { feature.fetch() }
  → loaders: StateFlow<ImmutableSet<LOADER>> includes MyLoader.LoadingData while running
    → Screen reads loaders and shows a spinner
```

## File-by-file rules

### `<Name>State.kt`

```kotlin
@Immutable
internal data class NoteEditorState(
    val tag: TagState? = null,
    val amount: AmountFormatState = AmountFormatState.Empty(),
    val history: ImmutableList<NoteState> = persistentListOf(),
)
```

Each property has a default value, so `NoteEditorState()` is the initial state passed to `BaseViewModel(...)` and to previews. No separate `companion object Empty` is needed.

Or, for a static/empty-data screen:

```kotlin
@Immutable
internal data object ProfileSettingsState
```

Or, for a multi-mode screen:

```kotlin
@Immutable
internal sealed interface AuthorizationState {
    @Immutable data class Idle(...) : AuthorizationState
    @Immutable data class Submitting(...) : AuthorizationState
    @Immutable data object Done : AuthorizationState
}
```

Rules:

- `@Immutable` (or `@Stable` for `sealed interface`). Marks Compose stability inference.
- Properties: `val`. No `var`.
- Collections: `ImmutableList`/`ImmutableSet`/`PersistentList` (kotlinx-collections-immutable).
- Strings: `UiText` if the value depends on resources/locale; `String` if it's a verbatim user input or a non-localizable identifier.
- Form fields: `*FormatState` (`EmailFormatState.Valid("a@b.com", "a@b.com")`, ...).
- Provide an initial state via default values on every property — `<X>State()` is then the "empty" instance used as `BaseViewModel`'s initial state and for previews.
- **No** direct domain model storage if a transformation is required — use `:data-mappers:domain-to-state` to map to a UI-friendly shape.

### `<Name>Direction.kt`

```kotlin
internal sealed interface NoteEditorDirection : BaseDirection {
    data object Back : NoteEditorDirection
    data object OpenSettings : NoteEditorDirection
    data class OpenNoteArchive(val initialRange: DateRange) : NoteEditorDirection
}
```

Rules:

- `sealed interface`, `internal`.
- Subtypes: `data object` for parameterless, `data class` for parameterized.
- Each subtype represents a single **navigation intent** (Back, OpenX, ShowDialog). NOT a state mutation — those happen in `update {}`.
- Subtypes can carry parameters needed by the parent to compose the actual navigation (`initialRange`).

### `<Name>Loader.kt`

```kotlin
@Immutable
internal sealed interface NoteEditorLoader : BaseLoader {
    @Immutable data object LoadingHistory : NoteEditorLoader
    @Immutable data object SavingAmount : NoteEditorLoader
}
```

Rules:

- `@Immutable sealed interface`, `internal`.
- Subtypes: `@Immutable data object`. Distinct concurrent ops get distinct subtypes (so the UI can show separate spinners).
- Empty loader is allowed: `@Immutable internal sealed interface FooLoader : BaseLoader` with no subtypes. Used when no async ops exist.

### `<Name>Contract.kt`

```kotlin
@Immutable
internal interface NoteEditorContract {
    fun onBack()
    fun onAmountPickerClick()
    fun onSettingsClick()
    fun onApplyClick()

    companion object Empty : NoteEditorContract {
        override fun onBack() = Unit
        override fun onAmountPickerClick() = Unit
        override fun onSettingsClick() = Unit
        override fun onApplyClick() = Unit
    }
}
```

Rules:

- `@Immutable interface`, `internal`.
- Methods: `onXxx*()` — past or present tense, describes a UI event ("user tapped X").
- All return `Unit` (or rarely `Boolean` for short-circuit cases).
- `companion object Empty` provides no-op defaults for previews.
- **No** methods that return data — UI reads data from `State`, never from the contract.

### `<Name>ViewModel.kt`

```kotlin
internal class NoteEditorViewModel(
    private val tagFeature: TagFeature,
    private val noteFeature: NoteFeature,
    private val dialogController: DialogController,
) : BaseViewModel<NoteEditorState, NoteEditorDirection, NoteEditorLoader>(
    NoteEditorState(),
), NoteEditorContract {

    init {
        tagFeature.observeTag()
            .onEach { tag -> update { it.copy(tag = tag) } }
            .safeLaunch()

        noteFeature.observeNotes()
            .onEach { history -> update { it.copy(history = history.toState()) } }
            .safeLaunch()

        safeLaunch(loader = NoteEditorLoader.LoadingHistory) {
            noteFeature.getNotes().getOrThrow()
        }
    }

    override fun onBack() {
        navigateTo(NoteEditorDirection.Back)
    }

    override fun onAmountPickerClick() {
        val current = (state.value.amount as? AmountFormatState.Valid)?.value
        dialogController.show(
            DialogConfig.AmountPicker(
                initial = current,
                onResult = { newValue ->
                    update { it.copy(amount = AmountFormatState.of(newValue)) }
                }
            )
        )
    }

    override fun onSettingsClick() {
        navigateTo(NoteEditorDirection.OpenSettings)
    }

    override fun onApplyClick() {
        val amount = (state.value.amount as? AmountFormatState.Valid)?.value ?: return
        safeLaunch(loader = NoteEditorLoader.SavingAmount) {
            noteFeature.updateAmount(amount).getOrThrow()
        }
    }
}
```

Rules:

- `internal class`, extends `BaseViewModel<State, Direction, Loader>`, **implements** `Contract`.
- Constructor takes only `Feature` / `UseCase` / `Controller` types — **no** raw services (`<Product>Api`, `Database`, ...).
- Initial state in the `BaseViewModel` constructor.
- `init { }` block: subscribe to `Flow`s using `.safeLaunch()` (extension); kick off any initial loads with `safeLaunch(loader = ...) { ... }`.
- Mutate state via `update { it.copy(...) }`. Never assign to a `var`.
- Emit navigation via `navigateTo(Direction.Foo)`.
- Run async work via `safeLaunch { ... }` or `Flow.safeLaunch()`. Never `viewModelScope.launch`, `GlobalScope`, or raw `CoroutineScope`.
- Catch exceptions **only** for domain-level success/failure (`result.onSuccess { ... }`); raw `try/catch` is forbidden — errors flow through `safeLaunch` → `ErrorProvider` automatically.

See `04-base-classes/01-base-viewmodel.md` for the full `BaseViewModel` API.

### `<Name>Component.kt`

```kotlin
internal class NoteEditorComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
    private val toSettings: () -> Unit,
    private val toNoteArchive: (DateRange) -> Unit,
) : BaseComponent<NoteEditorDirection>(componentContext) {

    override val viewModel: NoteEditorViewModel = componentContext.retainedInstance {
        NoteEditorViewModel(
            tagFeature = getKoin().get(),
            noteFeature = getKoin().get(),
            dialogController = getKoin().get(),
        )
    }

    override suspend fun eventListener(direction: NoteEditorDirection) {
        when (direction) {
            NoteEditorDirection.Back -> back()
            NoteEditorDirection.OpenSettings -> toSettings()
            is NoteEditorDirection.OpenNoteArchive -> toNoteArchive(direction.initialRange)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        NoteEditorScreen(state.value, loaders.value, viewModel)
    }
}
```

Rules:

- `internal class`, extends `BaseComponent<Direction>(componentContext)`.
- Constructor takes `componentContext` plus **callback lambdas** for every navigation action this Component cannot perform itself (typically: `back`, `close`, and any cross-feature nav).
- `viewModel` is created via `componentContext.retainedInstance { <Name>ViewModel(getKoin().get(), ...) }`. Dependencies are pulled from Koin — **not** threaded through the Component constructor.
- `eventListener` is `when (direction) { ... }` mapping every `Direction` subtype to a constructor lambda call (`back()`, `toSettings()`) or a child-component `navigation.push(...)` call if this is a parent component owning a stack.
- `Render()` collects state + loaders multiplatform-safely and calls the `Screen` function.

### `<Name>Screen.kt`

```kotlin
@Composable
internal fun NoteEditorScreen(
    state: NoteEditorState,
    loaders: ImmutableSet<NoteEditorLoader>,
    contract: NoteEditorContract,
) {
    BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.screen)) {
        Toolbar(
            title = AppTokens.strings.res(Res.string.<key>),
            leading = Leading.Back(onClick = contract::onBack),
        )

        // ... actual content
    }
}

@AppPreview
@Composable
private fun NoteEditorScreenPreview() {
    PreviewContainer {
        NoteEditorScreen(
            state = NoteEditorState(
                amount = AmountFormatState.of(72.0),
                history = stubNotes(),
                tag = stubTag(),
            ),
            loaders = persistentSetOf(),
            contract = NoteEditorContract.Empty,
        )
    }
}
```

Rules:

- `@Composable internal fun`. **Three** arguments: `state`, `loaders`, `contract`. Optionally a `component: <Name>Component` if the screen needs to render a child stack/slot.
- Wraps content in `BaseComposeScreen(background = ...)` (or `BottomSheet` content for dialogs).
- Reads all strings/colors/dp via `AppTokens.*`.
- Computes derived state via `remember(state.x, state.y) { ... }`.
- Has a `@AppPreview private fun <Name>ScreenPreview()` immediately below the screen function, wrapped in `PreviewContainer { ... }` with stub data + `Contract.Empty`.
- **No** `LaunchedEffect(Unit)` for navigation. Navigation goes via `Direction` only.
- **No** business logic. Only rendering and event forwarding via `contract.onXxx*()`.

## Why all seven files

| Concern | File | Why separated |
|---|---|---|
| What the user sees | `State` | Immutable; recomposable; survives process death (if `@Serializable`) |
| What the user can do | `Contract` | Stable callback set; testable in isolation; previewable |
| What the screen is doing | `Loader` | Multiple concurrent loaders coexist; UI shows specific spinners |
| Where to go next | `Direction` | Decouples VM from navigation library; testable in isolation |
| Logic | `ViewModel` | One place for state mutation + side effects; lifecycle-aware |
| Wiring | `Component` | Decompose-specific; survives configuration changes; handles result subscriptions |
| Rendering | `Screen` | Pure UI; no logic; trivially previewable |

Combining them (e.g. "the ViewModel implements the State" — common in MVVM) ties orthogonal concerns together and makes refactoring expensive. The seven-file pattern is the **uniform** cost: every feature looks the same; new contributors learn it once.

## What goes where (quick reference)

| Where | Allowed | Forbidden |
|---|---|---|
| State | `@Immutable`, immutable collections, `UiText`, `*FormatState` | `String` for localizable values, mutable collections, domain models requiring mapping |
| Direction | Nav intents only | State mutations, side effects |
| Loader | Op tags | Anything else |
| Contract | UI callbacks | Methods returning data, mutable state |
| ViewModel | `update`, `navigateTo`, `safeLaunch`, `withLoader` | `viewModelScope`, `GlobalScope`, raw `try/catch`, Compose code |
| Component | Decompose wiring, DI pulls, `eventListener` | UI rendering, business logic |
| Screen | Composables, derived state via `remember` | State mutations, side effects beyond `LaunchedEffect` for non-nav purposes |
