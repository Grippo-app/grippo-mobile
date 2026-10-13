# Screen results and ViewModel data flow

## Agreed project convention

For a screen returning data, use the existing `BaseComponent.observeResult` /
`sendResult` API. Dialogs keep their existing `DialogConfig.onResult` callbacks
and session `finish(result)`; do not convert them to screen results.

### The receiver owns the key

- The receiving screen chooses the key and registers its observer before opening
  the producer. Define fixed typed keys as `val` in the receiver ViewModel's
  `companion object`; define an instance-specific key with a companion function
  taking the stable entity ID. Use the same definition for subscription and launch.
- Pass an explicit, immutable `resultKey: ResultKey<Result<Payload>>` through the producer's
  `@Serializable` route, navigation Directions/callbacks and Component constructor.
  Preserve it during route restoration. Use the same typed key directly in
  `observeResult` / `sendResult`; do not unwrap and recreate it. Do not provide a
  producer-owned fallback key.
- The producer sends to the supplied key; it must not know the caller identities,
  introduce an `Origin` enum or infer the caller from the active/previous stack entry.
- All variants of one result protocol use that key. Protocol Actions stay nested
  under the producer's route; `Result<T>` wraps the payload automatically.
- Keys are global across live components. Scope keys by stable receiver identity
  when several instances can coexist; a fixed constant is suitable only for a
  receiver that is unique in that flow. The same key must always mean the same type.

Current examples: `TrainingRecordingViewModel.LIBRARY_RESULT_KEY` and
`EXERCISE_RESULT_KEY`, `TrainingExerciseViewModel.libraryResultKey(exerciseId)`,
`ExercisesRouter.Library.resultKey`, and `TrainingRouter.Exercise.resultKey`.

```kotlin
// Receiver ViewModel
companion object {
    val EDITOR_RESULT_KEY: ResultKey<Result<NotesRouter.Note.Action>> =
        ResultKeys.create("notes-editor")
}

// Receiver Component: register once in init, before launching the editor.
observeResult<Result<NotesRouter.Note.Action>>(
    key = NotesListViewModel.EDITOR_RESULT_KEY,
    onResult = { result ->
        when (val action = result.data) {
            is NotesRouter.Note.Action.Sync -> viewModel.updateNote(action.note)
            is NotesRouter.Note.Action.Remove -> viewModel.removeNote(action.id)
        }
    },
)

// Receiver launches a serializable route containing its key.
NotesRouter.Note(note = note, resultKey = EDITOR_RESULT_KEY)
```

### The sender requests Back

The ViewModel emits a data-bearing Direction. The sending Component handles it
with `sendResult`, then its ordinary constructor-injected `back` callback:

```kotlin
is NoteEditorDirection.Save -> {
    sendResult(resultKey, NotesRouter.Note.Action.Sync(direction.note))
    back.invoke()
}
NoteEditorDirection.Back -> back.invoke()
```

- Use this same `sendResult → back` shape for Library selection and editor Save.
  Updates that keep the screen open send data without Back.
- The receiver observer updates its own ViewModel. If receiving requires a new
  screen, the ViewModel emits the normal `ToExercise`/`ToEditor` Direction.
  Never call Back in an observer to close the sending screen.
- The parent handles the ordinary Back via its existing navigation chain and `pop()`.
  Do not introduce a parent `pop { success -> sendResult(...) }` relay, a second
  result key, special replacement Direction or `closeWithResult` callback for
  this standard return scenario.
- Do not add hidden runtime flags such as `selectionSent`, `resultHandled` or
  `isClosing` to suppress repeat selection/navigation. Use existing navigation
  and lifecycle mechanisms; do not disguise the flag as a Job, set or wrapper.
- `sendResult` returns before observers necessarily finish. This convention is
  not a synchronous transaction or an exactly-once guarantee. Verify the actual
  navigation chain for a receiver that immediately opens another screen. Do not
  claim a race exists, or promise that it cannot exist, without evidence.

## ResultKey serialization

Use the existing foundation `ResultKey<T : BaseResult>`, not a second key wrapper
or a raw String contract. `ResultKeys.create<Result<Payload>>(address)` creates it.
The type parameter connects sender, route and observer at compile time.
`ResultKeySerializer` encodes only the address as a String; it does not serialize
`Result` or its payload. This preserves the previous String route representation
and allows non-serializable Action protocols. Runtime matching still uses the
address, so the type parameter does not protect against manually reusing one
address with incompatible payload types.

## Existing bus semantics

`ResultManager` is internal to foundation; feature code never injects it or
creates a second result bus. The Component owns result observation and emission,
not the ViewModel or Composable.

- `ResultEmitter` is a singleton `MutableSharedFlow<Pair<String, BaseResult>>`
  with no replay and `extraBufferCapacity = 64`, exposed as `Flow`.
- Emission uses suspending `emit`, not dropping `trySend`. A full buffer can suspend
  pending sends; a slow subscriber can create backpressure across keys.
- All subscribed collectors see emissions and filter by key. Different-key
  observers cannot steal events. Same-key observers all receive the result;
  accidental duplicate subscriptions can therefore duplicate handling.
- `ResultManager.observeResult` starts with `CoroutineStart.UNDISPATCHED` to
  subscribe before returning. Register once per key in Component `init` on Main,
  not from Render, repeated click handlers or repeated lifecycle callbacks.
- `sendResult` launches with `UNDISPATCHED` and prevents emission from an already
  cancelled sender. It may suspend; do not treat UNDISPATCHED as synchronous completion.
- `BaseComponent` uses `Main.immediate`; observers remain subscribed while the
  component is hidden. On destruction, subscriptions and pending sends are canceled.
  Cancellation does not retract a result already committed to the shared buffer.
- No replay or durable delivery: a new observer does not receive an earlier result.
  Restored routes retain keys, not pending result payloads. Re-subscribe normally
  when components are recreated; do not invent replay caches or subscription registries.
- Keep observers short. An unhandled exception can terminate that observer.

## Verification for changes to screen results

Check caller-chosen key delivery for every result Action, no delivery to an
unrelated key, and key serialization/restoration. For Library-like flows check
add, replace, cancellation/system Back, retained caller state and navigation after
selection. Exercise both queued and immediate coroutine handling when navigation
order matters. Review actual producers and consumers; a constant definition alone proves no behavior.

## Anti-patterns

- Hardcoded receiver keys inside producers, or duplicated raw key strings at
  subscription and launch sites.
- Producer-side caller mappings, reading/casting the active route to discover the
  receiver, or guessing from the previous stack entry.
- Multiple result relays/keys for a single return, or sending navigation commands
  as result payloads instead of ordinary Directions.
- Closing the sending screen from the receiving observer.
- Hidden once-only runtime flags and new result infrastructure for this scenario.
- Re-registering an observer or treating the bus as durable/replayed messaging.

## Foundation source of truth

Inspect the existing implementation before changing its mechanics:
- [ResultEmitter.kt](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/internal/result/ResultEmitter.kt)
- [ResultManager.kt](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/internal/result/ResultManager.kt)
- [BaseComponent.kt](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/BaseComponent.kt)

Do not recreate these classes from a generic template.

---

## Data boundary for result producers and receivers

ViewModels consume the actual Feature/UseCase contracts, not backend/DAO/repository
implementations. Preserve the declared error/return semantics: unwrap Result calls
inside safeLaunch; direct-value throwing commands remain direct. Observations use
the existing Flow.safeLaunch pipeline and may fail; navigation/result delivery
must not manufacture success when a required operation fails.
Business-model conversion remains in directional data-mappers. Result ownership
and sendResult → back are independent of the business API's method naming.
