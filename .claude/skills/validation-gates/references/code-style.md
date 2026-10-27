# Code style and naming (owner)

This reference owns Grippo's cross-cutting Kotlin style. Owning
skills link here; they do not restate these rules. Current source establishes
actual signatures; an existing deviation does not weaken a rule.

## Visibility and files

- `explicitApi()` is on: every public declaration says `public`. Implementations
  and helpers stay `internal`/`private`; real module-boundary contracts, entry
  Components, State models, shared widgets and DI modules may be public.
  Implementations use the `Impl` suffix.
- Independent new types get separate files. Related sealed variants and an
  existing coherent model family may share a file; do not split or merge types
  only to satisfy a template.
- Put a calculation or helper at its owning type as an ordinary method or
  computed property; use the companion for factories. Do not append private
  receiver extensions or unrelated utility functions to a State, ViewModel,
  UseCase or Screen file. Mapper file content follows
  [mappers](../../mappers/SKILL.md#strict-rules). Other top-level extensions
  belong in dedicated, purpose-named utility files. Shared model preview data follows
  [the stub convention](../../ui-feature/references/state.md#top-level-stub-functions);
  Compose decomposition follows its owning skill.
- UI-emitting composables are `PascalCase`; value-returning composable helpers
  are `lowerCamelCase`. A helper that retains its result or object identity uses
  `remember<Noun>` and keys every changing input, including locale. Preserve the
  naming family of the owning API and migrate every caller when renaming it.
  ViewModel callbacks are `onXxx` (`onApplyClick`, `onBack`). Resource keys are
  descriptive `snake_case`.
- Templates by role:
  - DTO: `<Name>Response` / `<Name>Body`, `@Serializable`, every field nullable
    `= null` with `@SerialName`.
  - Entity: `<Name>Entity`.
  - State: `@Immutable internal data class <Feature><Screen>State` (or
    `data object` / `sealed interface`).
  - Direction: `internal sealed interface <Feature>Direction : BaseDirection`.
  - Loader: `@Immutable internal sealed interface <Feature>Loader : BaseLoader`.
  - Contract: `@Immutable internal interface <Feature>Contract { companion object Empty }`.
  - New use case: `public class <Verb><Noun>UseCase` with one public method;
    command contracts follow
    [throwing suspend operations](../../data-layer/references/operation-outcomes.md#throwing-suspend-contracts-canonical).

## Packages

- Base scheme `com.grippo.<area>.<feature>` (`com.grippo.profile.body`).
- Some existing directories use dotted segments (`com/grippo/data.features.trainings/`,
  `com/grippo/dialog.api/`); mappers, services and newer modules use
  slash-separated directories. Preserve the layout of the module you are in;
  new modules use slash-separated directories. Never normalize unrelated paths.
- Helpers stay `internal`/`private` in the owning package; service cores keep
  their `internal/` package layout.

## Kotlin blocks

- Keep function, `init`, control-flow and business blocks multiline, including
  every `safeLaunch { }` and `analytics.trackOperation { }` body, even for one
  call. Keep short single-expression state updates and simple Flow operators
  compact: `update { it.copy(value = value) }`, `.map { it.range.value }`,
  `.filter { it.isSelected }`, `.flatMapLatest { it }`. Expand multi-operation
  lambdas. Empty lambdas such as `ErrorRecovery.Close(onClose = {})` may stay inline.
- Declarations stay contiguous: constructor and function parameters, model and
  State properties, interface members, token, palette and style fields follow
  each other without blank lines, annotated and multi-line members included. A
  blank line separates only parts that are not related at all - for example
  individual definitions and the aggregate list built from them, or threshold
  families of different rules. Blank lines between stages inside function, `init`
  and handler bodies follow [ViewModel layout](#viewmodel-layout).
- No named local function declarations inside `init`, constructors, functions
  or lambdas (`fun helper()` nested in a body). Ordinary callbacks, lambdas and
  DSL blocks are fine. Declare methods at class scope or functions at file scope.
  An operation that `init` and another member both run is one private
  class-level method that both call; it is not a local function and not a copy
  of the sequence. A failed startup operation is not such a reason: the error
  popup's Retry button already runs the failed `safeLaunch` block again (see
  [Retry and close](../../ui-feature/references/error-pipeline.md#retry-and-close)).
- ViewModel coroutines: only `safeLaunch` / `Flow.safeLaunch`. Never
  `viewModelScope.launch`, `GlobalScope`, `runBlocking`, raw `CoroutineScope`,
  a manual `CoroutineExceptionHandler` or `async { }.await()` for a single task.
- Call throwing commands directly inside `safeLaunch`; no local `try/catch`,
  `runCatching`, `onFailure`, `recover`, `fold` or `getOrElse` that hides or
  presents an unhandled failure in a ViewModel. Direct credential-adapter calls
  and the shared failure path follow the
  [UI error pipeline](../../ui-feature/references/error-pipeline.md).
- Collections in `@Immutable` State and Loaders: only `kotlinx-collections-immutable`
  (`ImmutableList`, `ImmutableSet`, `PersistentList`). `List`/`Set`/`Map` and
  mutable collections are forbidden there.
- Dispatchers: `Dispatchers.Main.immediate` (default) for state, dialogs and
  navigation; `Dispatchers.IO` only for explicit heavy IO inside a ViewModel
  (rare; IO lives in repositories and `BackendClient.invoke`); `Dispatchers.Default`
  for CPU-heavy work. Do not move light glue to another dispatcher.
- Cancellation: use owned scopes and cancellable APIs, preserve
  `CancellationException`; add `ensureActive()` only where a real cancellation
  gap exists (noncooperative CPU work or an SDK boundary). See
  [forbidden patterns](forbidden-patterns.md#coroutines).

## ViewModel layout

Every `<Name>ViewModel` (screen or dialog) reads top to bottom in the same
order: constructor, `init`, private handlers, Contract callbacks. For
orientation on the constructor and the `init` statement list inspect
`ExerciseViewModel` (dialog: caller argument) and `ProfileBodyViewModel`
(screen: draft-restoring constructor); their positional analytics calls,
inline `update` branches, source calls on the second chain line and missing
blank line before `update` predate this section and are not a model. The
excerpt at the end of this section is schematic and uses placeholder names.

### Constructor

Parameters come in four groups, in this order. Omit a group the ViewModel does
not need; never add a parameter only to fill a group.

1. Caller-supplied inputs from the route or the dialog config (`id`, `mode`,
   `initial`, `range`).
2. `private val analytics: <Feature>Analytics`.
3. Injected dependencies: `*Feature`, `*UseCase`, `DialogController`,
   `StringProvider`, platform managers.
4. `initialState` - the restored draft of a screen that preserves one under
   [state-preservation](../../state-preservation/SKILL.md). Dialogs have no
   restored state and therefore no fourth group.

A parameter is `private val` only when a method outside `init` and the initial
State expression reads it; a value consumed only there stays a plain parameter.
Apply the order when creating or changing a constructor and migrate its
`retainedInstance` caller in the same change.

### `init`

`init` is a flat list of statements separated by one blank line. Only these
statements are allowed there:

- a Flow chain ending in `.safeLaunch(...)`: the source call on the first line,
  one operator per line below it, the value handed to a handler by method
  reference (`.onEach(::provideNote)`);
- a `safeLaunch { }` block for a startup load: it either binds the returned
  value to a local and passes it to a handler, or discards it when a Flow chain
  in the same `init` delivers the data (cache refresh). Instrumentation follows
  the [analytics observation boundary](../../feature-analytics/SKILL.md#observation-boundary-must);
- a call to a private class-level method that another member also calls
  ([Kotlin blocks](#kotlin-blocks)); a startup operation with no other caller
  is written in `init` directly;
- an `if (initialState == null)` or a `when` over a caller-supplied input whose
  branches contain only the statements above (one-time population under
  [state-preservation](../../state-preservation/SKILL.md#data-backed-form-synchronization),
  a dialog config with several modes);
- a `val` holding a source Flow that two or more chains share.

Keep data-to-State conversion and assembly out of `init`: a
`.map { it.toState() }` inside an `init` chain is the conversion this rule
forbids; the mapper call belongs in the handler.

### Handlers

A private `provide<Noun>(value)` method turns one domain value into State. Its
body is a sequence of stages separated by blank lines - each stage one `val`
named after what it holds (`exercisesState`, `muscleLoad`), produced by a
mapper (`toState()`), a use case or a collection operation - and ends with
exactly one `update`, separated from the last stage by a blank line even when
there is only one stage. Contract callbacks (`onXxx`) use the same stage
shape: guards, then the analytics event, then the business step or
`navigateTo`; a callback body of two statements stays without blank lines.

### `update`

- Compute the new values before `update`; the lambda only copies them in, on
  one line: `update { it.copy(recent = recent, volume = volume) }`. When that
  line would exceed the IDE's Kotlin line length (120 characters), wrap it as
  `update {` / `it.copy(` / one argument per line / `)` / `}`; the lambda still
  only copies.
- Expand to a multiline lambda only when the next state depends on the current
  one. Then name the parameter (`update { current -> ... }`), derive locals
  from `current`, leave early with `return@update current` and end with one
  `current.copy(...)`.
- Never call a mapper, use case or feature inside the lambda; it runs under the
  compare-and-swap and may repeat.

### Named values and arguments

- Bind every intermediate result to a descriptive local before passing it on;
  do not nest a conversion or use-case call inside another call's argument
  list: `val history = noteFeature.getHistory(id)` and then
  `provideHistory(history)`.
- Use named arguments for State constructors, `copy`, analytics and dialog
  config models and every call with two or more arguments; a trailing lambda
  does not count (`safeLaunch(loader = NoteLoader.History, recovery = ErrorRecovery.Retry) { }`,
  `analytics.trackOperation(NoteOperation.LoadHistory) { }`). A single argument
  whose meaning the callee's name states stays positional (`provideNote(note)`).
  The foundation hook `analytics.presentScreen(<Feature>Screen.X, reason)` keeps
  the shape [feature-analytics](../../feature-analytics/references/viewmodel.md)
  shows.

```kotlin
// Schematic; Note* types are placeholders, not project APIs.
internal class NoteViewModel(
    id: String,
    private val analytics: NoteAnalytics,
    private val noteFeature: NoteFeature,
    private val noteVolumeUseCase: NoteVolumeUseCase,
    initialState: NoteState?,
) : BaseViewModel<NoteState, NoteDirection, NoteLoader>(
    initialState ?: NoteState(id = id)
), NoteContract {

    init {
        noteFeature.observeNote(id)
            .onEach(::provideNote)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        safeLaunch(loader = NoteLoader.History, recovery = ErrorRecovery.Retry) {
            analytics.trackOperation(NoteOperation.LoadHistory) {
                val history = noteFeature.getHistory(id)
                provideHistory(history)
            }
        }
    }

    private fun provideNote(value: Note?) {
        val note = value?.toState() ?: return

        update { it.copy(note = note) }
    }

    private fun provideHistory(value: List<NoteRevision>) {
        val history = value.toState()

        val volume = noteVolumeUseCase
            .fromRevisions(value)
            .toState()

        update { it.copy(history = history, volume = volume) }
    }

    override fun onBack() {
        analytics.trackEvent(NoteEvent.BackRequested)
        navigateTo(NoteDirection.Back)
    }
}
```

## Compose

Shared Compose API, structure and runtime rules belong to
[compose-components](../../compose-components/SKILL.md); feature integration belongs
to [UI composition](../../ui-feature/references/compose-rules.md); product values and
visual implementation belong to [design-system](../../design-system/SKILL.md).

Global experimental opt-ins live in
[the convention](../../../../build-logic/convention/src/main/kotlin/KotlinMultiplatformConventionPlugin.kt);
do not add source-level opt-ins for APIs it already covers.

## Comments

Source carries no comments and no KDoc; names, types and structure carry the
meaning. This covers Kotlin sources, Gradle Kotlin scripts and Swift in
`iosApp/`. What a comment would say lives at its owner: role-to-token choices
in [`design-system/CONSISTENCY.md`](../../../../design-system/CONSISTENCY.md),
analytics and preservation facts in the [COVERAGE records](../../../../CLAUDE.md#records),
rules in the [owning skills](../../../../CLAUDE.md#skills-index), verification
notes and trade-offs in the [task report](../SKILL.md#final-response-format).

- MUST NOT write a comment or KDoc in code a change adds or rewrites, with one
  exception: a single `//` line at the exact place where the code deliberately
  departs from a project canon or works around a platform or library behavior
  that a reader would otherwise "fix" and break. The line states that reason in
  one short sentence: what breaks if the code is changed.
- The exception never covers multi-line explanations, KDoc blocks, narration of
  what the code does, `TODO`/`FIXME`, task identifiers or commented-out code.
- Generated code and third-party license notices are exempt.
- `BaseViewModel` keeps its section separators (`// ------------ STATE API ------------`
  and its siblings); they are the only structural comments allowed, and only there.
- Existing comments in code a change does not rewrite are pre-existing
  deviations; remove them in a separate cleanup only when the user asks.
- A `TODO`/`NotImplementedError` path is never reported as finished work.
- Comments inside rulebook code blocks annotate the example; they are not part
  of the pattern to copy.
