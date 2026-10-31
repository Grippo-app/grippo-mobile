# MVI Contract (the seven files)

> Note/Tag/AmountPicker/NoteDetail examples are educational, nonexistent APIs. They are not the current route, dependency or source catalog; inspect real callers and signatures before adapting them.

Inspect actual neighboring [Login files](../../../../ui-screen-features/authorization/src/commonMain/kotlin/com/grippo/authorization/login/) for screen ownership and [ProfileComponent](../../../../ui-screen-features/profile/src/commonMain/kotlin/com/grippo/profile/ProfileComponent.kt) for a container. Their existing deviations do not waive the gates below.

Every screen and dialog is a package with **seven files** of the same template:
same names, same shape, same flow of data.

```
<feature>/
  <Name>Component.kt       // Decompose Component; owns ViewModel; handles Direction
  <Name>Contract.kt        // @Immutable interface with on*-callbacks + companion object Empty
  <Name>State.kt           // @Immutable data class | data object | sealed interface
  <Name>Direction.kt       // sealed interface : BaseDirection (navigation intents)
  <Name>Loader.kt          // @Immutable sealed interface : BaseLoader (active operations)
  <Name>ViewModel.kt       // BaseViewModel<State, Direction, Loader>, implements Contract
  <Name>Screen.kt          // @Composable internal fun (state, loaders, contract)
  components/              // optional — extracted feature composables
```

`<Name>` is the screen/dialog name (e.g. `NoteEditor`, `AmountPicker`). Files are
siblings in the same package. `Screen` is the **only** UI file among the seven.
Apply the [Compose extraction criteria](../../compose-components/references/structure-and-runtime.md#read-from-top-to-bottom)
to its composition and helpers; [feature file layout](module-structure.md#file-layout-for-a-feature)
owns the placement of extracted composables.

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
    val weight: WeightFormatState = WeightFormatState.Empty(),
    val history: ImmutableList<NoteState> = persistentListOf(),
)
```

In this example each property has a meaningful default, so `NoteEditorState()`
can initialize `BaseViewModel(...)`. Other states may require explicit initial data.
**No separate `companion object
Empty` is needed on State** (Empty belongs on `Contract`).

Static/empty-data screen: `@Immutable internal data object ProfileSettingsState`.
Multi-mode screen with distinct editor and archive layouts:

```kotlin
@Immutable
internal sealed interface NoteContentState {
    @Immutable
    data class Editor(val note: NoteState) : NoteContentState
    @Immutable
    data class Archive(val notes: ImmutableList<NoteState>) : NoteContentState
}
```

Operation progress stays in `*Loader` and VM `loaders` under
[State conventions](state.md#state-conventions).

Rules:
- `@Immutable` on State and every sealed subtype; use the canonical [state conventions](state.md).
- Properties: `val`. No `var`.
- Collections: `ImmutableList`/`ImmutableSet`/`PersistentList` (kotlinx-collections-immutable).
- Strings: `UiText` if the value depends on resources/locale; `String` if it's a verbatim user input or a non-localizable identifier.
- Form fields reuse a suitable existing formatter where parsing/validation/display requires it; plain nonsecret free text without those needs may remain String. Follow [formatter rules](state.md#formatstate-rules-must--anti-patterns); do not create a type per field.
- Use meaningful constructor defaults where an initial empty/loading value exists; pass required data explicitly through initialState. A no-argument `<X>State()` is not mandatory. Shared models follow the same [initialization rule](state.md#state-conventions); previews use explicit fixtures, not fabricated production defaults.
- **No** direct domain model storage if a transformation is required — map via `:data-mappers:domain-to-state` to a UI-friendly shape.
- **No flat scalar piles.** Cluster related primitives into sub-states, fixed value sets into enums, mutually exclusive business/content modes into a sealed interface (see `references/state.md`).

### `<Name>Direction.kt`

```kotlin
internal sealed interface NoteEditorDirection : BaseDirection {
    data object Back : NoteEditorDirection
    data object OpenSettings : NoteEditorDirection
    data class OpenNoteArchive(val initialRange: DateRange) : NoteEditorDirection
}
```

Rules:
- `sealed interface`, `internal` for a leaf; `public` when the owning Component is public (its `navigator` exposes the type).
- Subtypes: `data object` for parameterless, `data class` for parameterized.
- Each subtype is a single **navigation intent** (Back, OpenX, ShowDialog). NOT a state mutation — those happen in `update {}`.
- Subtypes can carry parameters needed by the parent to compose the actual navigation (`initialRange`).

### `<Name>Loader.kt`

```kotlin
@Immutable
internal sealed interface NoteEditorLoader : BaseLoader {
    @Immutable data object LoadingHistory : NoteEditorLoader
    @Immutable data object SavingWeight : NoteEditorLoader
}
```

Rules:
- `@Immutable sealed interface`, `internal` for a leaf; `public` when the owning Component is public (its `loaders` exposes the type).
- Subtypes: `@Immutable data object`. Distinct concurrent ops get distinct subtypes (so the UI can show separate spinners).
- Empty loader is allowed: `@Immutable internal sealed interface FooLoader : BaseLoader` with no subtypes. Used when no async ops exist.

### `<Name>Contract.kt`

```kotlin
@Immutable
internal interface NoteEditorContract {
    fun onBack()
    fun onWeightPickerClick()
    fun onSettingsClick()
    fun onApplyClick()

    companion object Empty : NoteEditorContract {
        override fun onBack() = Unit
        override fun onWeightPickerClick() = Unit
        override fun onSettingsClick() = Unit
        override fun onApplyClick() = Unit
    }
}
```

Rules:
- `@Immutable interface`, `internal`.
- Methods describe observed UI events ("user tapped X"); callback naming follows
  [code style](../../validation-gates/references/code-style.md). Preserve the
  existing container/root relay APIs when threading child callbacks through
  [cross-feature navigation](navigation.md#cross-feature-navigation).
- Action callbacks MUST return Unit. Boolean is permitted only for a synchronous
  handled/consumed input-event protocol; it MUST NOT query UI/business data.
- `companion object Empty` provides no-op defaults for previews.
- **No** methods that return data — UI reads data from `State`, never from the contract.

### `<Name>ViewModel.kt`

Opening a new picker follows [dialogs.md](dialogs.md): its config has caller-owned title, typed content/result callbacks and exhaustive content comparison.

Rules:
- `internal class` for a leaf; `public class` when the owning Component is public (dialogs and feature roots expose `viewModel`, inspect `WeightPickerViewModel`/`ProfileViewModel`). Extends `BaseViewModel<State, Direction, Loader>`, **implements** `Contract`.
- Constructor receives caller route inputs, typed feature analytics,
  `Feature` / `UseCase` / `Controller` dependencies. Add a restored `initialState`
  only for screen drafts required by
  [state-preservation](../../state-preservation/SKILL.md#ownership-and-implementation).
  No direct API/DAO/repository dependencies (observe Flows, call throwing suspend
  commands - see [data boundary](results.md#data-boundary-for-result-producers-and-receivers)).
  Authorization's documented
  Google/Apple credential-provider exception stays local to that feature.
- Constructor parameter order, the `init` layout (including
  [where long-lived observations start](../../validation-gates/references/code-style.md#init)),
  `provide*` handlers, the `update` shape and named arguments are owned by
  [ViewModel layout](../../validation-gates/references/code-style.md#viewmodel-layout);
  block style, the named-local-function ban and shared class-level methods by
  [Kotlin blocks](../../validation-gates/references/code-style.md#kotlin-blocks).
  Apply both as written there.
- Initial state in the `BaseViewModel` constructor.
- Mutate state via `update { it.copy(...) }`. Never assign to a `var`.
- Emit navigation via `navigateTo(Direction.Foo)`.
- Run async work via `safeLaunch { ... }` or `Flow.safeLaunch()`. **Never** `viewModelScope.launch`, `GlobalScope`, or raw `CoroutineScope`.
- Call throwing Feature/UseCase commands inside `safeLaunch`; do not hide or
  present unhandled failures with local catches or result failure handlers. Pure
  `mapError` changes only dialog presentation; the required `ErrorRecovery` follows
  [Retry and close](error-pipeline.md#retry-and-close). Suspend helpers
  propagate failures to the caller's `safeLaunch`; no local catches or
  telemetry-owned recovery. Requests driven by a latest-value source follow
  [the shared error rules](error-pipeline.md#requests-driven-by-a-long-lived-source).

(Full `BaseViewModel` API: `references/base-classes.md`.)

### `<Name>Component.kt`

Rules:
- A leaf inside its feature is `internal`; a feature/module entry Component is `public`, including a directly exposed single-screen feature or dialog. Extend `BaseComponent<Direction>(componentContext)`.
- Constructor takes `componentContext` plus **callback lambdas** for every navigation action this Component cannot perform itself (typically: `back`, `close`, and any cross-feature nav).
- `viewModel` is created via `componentContext.retainedInstance { <Name>ViewModel(getKoin().get(), ...) }`. All dependencies, including stateless analytics, resolve inside that block, not through Component constructors. VM owns typed events/operations; every independent screen must declare screenPresentation, containers keep default null. BaseComponent/foundation binds it without feature wiring. See [feature analytics](../../feature-analytics/SKILL.md).
- `eventListener` is `when (direction) { ... }` mapping every `Direction` subtype to a constructor lambda call (`back()`, `toSettings()`) or a child-component `navigation.push(...)` call if this is a parent component owning a stack.
- `Render()` collects state + loaders multiplatform-safely and calls the `Screen` function.

### `<Name>Screen.kt`

Rules:
- `@Composable internal fun`. **Three** arguments: `state`, `loaders`, `contract`. Optionally a `component: <Name>Component` if the screen needs to render a child stack/slot.
- Wraps content in `BaseComposeScreen(background = ...)`.
- Reads all strings/colors/dp via `AppTokens.*`.
- Follow [recomposition discipline](../../compose-components/references/structure-and-runtime.md#stability-and-recomposition) for derived rendering values; simple checks stay inline.
- New or visually changed screens MUST have private `@AppPreview`s for the significant states ([definitions](../../../../CLAUDE.md#definitions)), wrapped in `PreviewContainer { ... }` with explicit fixture data and `Contract.Empty`. Nonvisual changes assess visual impact and reuse existing previews; they need no unrelated preview expansion. Shared model stubs stay in ui-core:state; private screen-State fixture builders may live with the preview.
- **No** `LaunchedEffect(Unit)` for navigation. Navigation goes via `Direction` only.
- **No** business logic. Only rendering and event forwarding via `contract.onXxx*()`.

## Why all seven files

| Concern | File | Why separated |
|---|---|---|
| What the user sees | `State` | Immutable; recomposable; initialized from saved drafts/route inputs, with data-backed fields rebuilt by the VM |
| What the user can do | `Contract` | Stable callback set; testable in isolation; previewable |
| What the screen is doing | `Loader` | Multiple concurrent loaders coexist; UI shows specific spinners |
| Where to go next | `Direction` | Decouples VM from navigation library; testable in isolation |
| Logic | `ViewModel` | One place for state mutation + side effects; lifecycle-aware |
| Wiring | `Component` | Decompose-specific; survives configuration changes; handles result subscriptions |
| Rendering | `Screen` | Pure UI; no logic; trivially previewable |

The seven-file pattern is the **uniform** cost: every feature looks the same.

## What goes where (quick reference)

| Where | Allowed | Forbidden |
|---|---|---|
| State | `@Immutable`, immutable collections, `UiText`, `*FormatState` | `String` for localizable values, mutable collections, domain models requiring mapping |
| Direction | Nav intents only | State mutations, side effects |
| Loader | Op tags | Anything else |
| Contract | UI callbacks | Methods returning data, mutable state |
| ViewModel | `update`, `navigateTo`, `safeLaunch`, `withLoader` | `viewModelScope`, `GlobalScope`, local catches, Compose code |
| Component | Decompose wiring, DI pulls, `eventListener` | UI rendering, business logic |
| Screen | Composables, derived rendering values, presentation/system effects with correct keys and teardown | Business state mutations, business operations or navigation effects |

## Architecture cross-cuts (always-on)

Keep the seven-file responsibilities and the [feature file layout](module-structure.md#file-layout-for-a-feature).
Read the canonical owner before changing its topic; this quick reference does not
replace that contract or the skill's mandatory analytics/preservation gates.

| Topic | Canonical owner |
|---|---|
| Typed routes, stack/container ownership and cross-feature navigation | [Navigation](navigation.md) |
| In-memory config/session/step wiring, callbacks and dismissal | [Dialogs](dialogs.md) |
| Receiver-owned typed keys, producer delivery and result-bus lifecycle | [Screen results](results.md) |
| Failure, cancellation, presentation mapping and recovery | [Error pipeline](error-pipeline.md) |
| Serializable route payloads, platform archives and excluded dialog sessions | [Process-death restoration](process-death.md) |
