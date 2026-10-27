# Error Pipeline

The `MyLoader`/`MyDirection`/`feature` examples are schematic; linked source files own real APIs.

Unhandled errors flow through a **single path**. Analytics observes the original
operation error without classifying it or changing recovery. Reporting respects core collection policy; ErrorProvider
translates failures into UI-friendly dialogs.

## The default pipeline

```
throw inside a safeLaunch attempt
  → BaseViewModel.attempt returns the failure        // CancellationException is rethrown
  → BaseViewModel.resolve(failure, retryable = recovery == ErrorRecovery.Retry, mapError)
  → AppLogger.General.error(...)                     // file log
  → CrashReporting.reportException(...)              // toolkit core collection policy
  → ErrorProvider.resolve(mapError(failure), retryable) // UI presentation; suspends for the decision
  → ErrorProviderImpl maps AppError → AppErrorState
  → DialogController.open(DialogConfig.ErrorDisplay(title, state, onRetry, onClose)), awaits the decision
  → ErrorDecision.Retry: next attempt in the same Job
  → ErrorDecision.Close: close(recovery), the Job completes
```

`onRetry` completes the decision with `ErrorDecision.Retry` and exists only when
`retryable` (`ErrorRecovery.Retry`); `onClose` completes it with `ErrorDecision.Close`.
The first completion wins. Each failure opens its own popup and awaits its own
decision; failures are not merged. `close(recovery)` runs `onClose` only for
`ErrorRecovery.Close`.

Every unhandled failure follows this path automatically. The ViewModel author writes only:

```kotlin
safeLaunch(loader = MyLoader.Fetch, recovery = ErrorRecovery.Retry) {
    feature.fetch()
}
```

…and an error dialog with a Retry button appears if it fails. No `try/catch`, no
manual dialog dispatching, no hand-written retry.

## `ErrorProvider` and the dialog mapping

Inspect the current [ErrorProviderImpl](../../../../ui-core/error/error-provider-impl/src/commonMain/kotlin/com/grippo/core/error/provider/impl/ErrorProviderImpl.kt) for exact mapping and callback fields.

`AppErrorState` is the UI mirror of `AppError`. It lives in `:ui-core:state` (package `core.state.error`) — **not** in `:ui-core:error:error-provider`, which is kept UI-free so `:data-services:*` and `:toolkit:http-client` can depend on `AppError` without dragging in UI. Inspect `AppErrorState.kt`; current shape: `Network.Expected(title, description?)`, `data object`s `Network.NoInternet`, `Network.Timeout`, `Network.IncompleteResponse` and `Network.Unexpected`, a top-level `Expected(title, description?)` declared `: Network`, and `data object Unknown`. Fields are plain `String`s; `title()`/`description()` build the dialog's `UiText`, and the four transport objects take both from localized resources, so an exception's English message never reaches the dialog. On the `AppError` side, `Network.IncompleteResponse` and `Unknown` are no-argument plain classes; the other subtypes are data classes. `ErrorDisplay` is a `DialogConfig` subtype in `:ui-dialog-features:dialog-api`.

## Inside `BaseViewModel` — `resolve`

Inspect [BaseViewModel](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/BaseViewModel.kt)
and [OperationManagerImpl](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/internal/operation/OperationManagerImpl.kt).
Current shape:

- **Block `safeLaunch`** starts one Job through `OperationManager.launch` and loops
  inside it over `withLoader(loader) { attempt(block) }`. `attempt` runs
  `supervisorScope { block() }`, rethrows `CancellationException` and returns any
  other failure. This foundation catch is the documented infrastructure boundary;
  feature code still MUST NOT catch. The loader is shown during each attempt, not
  while the popup waits.
- **`resolve(failure, retryable, mapError)`** (`private suspend`) runs
  `AppLogger.General.error(...)` → `crashReporting.reportException(failure)` →
  `error = if (failure is Exception) mapError(failure) else failure` →
  `errorProvider.resolve(error, retryable)`, which suspends until the user decides.
  On `ErrorDecision.Retry` the loop starts the next attempt; on `ErrorDecision.Close`
  it runs `close(recovery)` and the Job completes.
- **Flow `safeLaunch`** collects `source.retryWhen { ... }.catch { ... }`, where
  `source` is the `whileActive` wrapper (`WhileActive`) or the flow itself
  (`Infinity`). `retryWhen` calls `resolve` and re-collects the same source on
  `Retry`; `catch` rethrows `CancellationException` and otherwise runs
  `close(recovery)`; the collection has ended. A later activation does not
  restart a closed collection.
- **`OperationManagerImpl.launch`** still installs a `CoroutineExceptionHandler` that
  ignores `CancellationException`. It now receives only stray failures of the block
  overload: a failing child coroutine launched inside the block, or a failure of the
  pipeline itself. Those get a popup without Retry, then `close(recovery)`. In the Flow
  overload a failure of the pipeline inside `retryWhen` reaches `.catch`, which ends
  the collection with `close(recovery)` and no second popup; the original failure was
  already logged and reported.

Original failures are logged and reported; only `Exception` receives the pure
`mapError` for the dialog, and a fatal `Error` is not transformed. While the popup is
open, the Job stays active and suspended on the decision; cancellation follows
[Retry and close](#retry-and-close).

Keep the error contract unified across block and Flow `safeLaunch`: both take a
required `recovery: ErrorRecovery` with no default (inspect
`ui-core/foundation/.../models/ErrorRecovery.kt`; current shape: `data object Retry`
and `class Close(val onClose: () -> Unit)`) and `mapError: (Throwable) -> Throwable = { it }`,
with the same names and the shared error pipeline. Every call passes `recovery`
explicitly; the choice follows [Retry and close](#retry-and-close).
`mapError` defaults to returning the original error. Use it only when a concrete boundary
exception needs an existing `AppError` representation for the standard dialog.
Keep it pure and non-throwing: no requests, state changes, navigation, reporting,
retries or business fallback. Return unrecognized errors unchanged. Analytics,
logging and crash reporting must receive the original exception; only
`ErrorProvider` receives the presentation mapping.

## Authoring rules (MUST)

```kotlin
// Good — errors flow through the pipeline; the popup offers Retry
safeLaunch(
    loader = MyLoader.SavingForm,
    recovery = ErrorRecovery.Retry,
) {
    val body = state.value.form.toDomain()
    feature.save(body)
    navigateTo(MyDirection.Close)
}
```

```kotlin
// BAD — manual catch bypasses the shared error path
safeLaunch(recovery = ErrorRecovery.Retry) {
    try {
        feature.save(...)
        navigateTo(...)
    } catch (e: Throwable) {
        update { it.copy(error = e.message) }    // forbidden
    }
}
```

Do not hide, replace or present unhandled failures through local catches,
`runCatching`, `Result.onFailure`, `recover`, `fold` or `getOrElse` in a ViewModel
or its helpers. Call throwing Feature/UseCase commands directly inside `safeLaunch`.
Keep all required work inside the decorated business block, including direct throwing credential
adapter calls ([platform ownership](../../data-layer/references/auth-session.md#credential-adapters-canonical)). Use pure
`mapError` only for dialog presentation and `ErrorRecovery.Close.onClose` only for
what follows a closed popup.
Foundation's loader cleanup via `try/finally` and documented infrastructure boundaries,
including foundation's `attempt` catch, remain valid. Business contracts follow
[throwing suspend operations](../../data-layer/references/operation-outcomes.md#throwing-suspend-contracts-canonical).

## Requests driven by a long-lived source

A request that follows a latest-value source (a selected period or range) is a plain
suspend step of that source's chain: `.mapLatest(::refreshX)` collected by one
`Flow.safeLaunch(recovery = ErrorRecovery.Retry)`, with `trackOperation` (and
`withLoader` when the screen shows a loader) inside the step. Inspect
`refreshTrainings` in `HomeViewModel`, `TrainingsViewModel` and
`TrainingCompletedViewModel`. A new value cancels the running request, and
`withLoader` clears its loader on cancellation.

A failed request fails the whole chain. Retry re-collects it and requests the
current value again; closing the popup ends it, so later selections no longer refresh from the
server until the ViewModel is recreated. This is the owner's accepted trade-off: the
separate Room observation chains keep showing cached data for every selection.
Do not isolate the request in a nested `safeLaunch` that the handler joins and
cancels in `finally`, and add no member Job, retry loop, stage flag or new scope.

## Splash profile loading

Inspect `ui-screen-features/authorization/.../splash/SplashViewModel.kt`.
Its startup `safeLaunch` with `AppContent` loads the reference catalogs and calls
`resolveDirection()` in that same coroutine. Onboarding and token presence select
Onboarding/AuthProcess first; only an existing session reaches `hasProfile()`.

`hasProfile()` decorates its cached-user read and required remote `getUser()` with
SessionCheck. Its Boolean selects Home or ProfileCreation. Lookup failures use the
shared log/crash/dialog pipeline and do not become an absent profile. The block
uses `ErrorRecovery.Retry`: Retry runs the complete startup sequence again, and
closing the popup leaves Splash on screen; Back then leaves through Splash's
existing back path. Session-loss navigation follows its
[owning session observer](navigation.md#direction--navigation-translation).

## Retry and close

Retry is the user's choice, never automatic. The popup's Retry button starts the next
attempt of the failed `safeLaunch` inside the same Job: the block overload re-runs the
same block with the same loader and recovery, the Flow overload re-collects the same
source, including its `WhileActive` wrapper. Feature ViewModels add no retry loop,
backoff or analytics retry policy; infrastructure retries with attempt limits stay in
their owning layer (for example the backend TokenProvider).

- **`ErrorRecovery.Retry`** is the choice for every operation that can safely run
  again. The popup offers Retry; closing it ends the operation with no continuation,
  and the screen stays as the failure left it (Splash stays on screen until system
  Back exits; Onboarding keeps an unknown consent, so Get started and Back stay
  inactive until the app restarts after a local storage failure; TrainingExercise keeps
  its last validation hints; a failed Flow collection stays ended). Values captured before `safeLaunch` are reused; read state
  inside the block when a retry must see current state. Captured UI-bound handles
  (Login's Google/Apple UI context, the Completed share capture) are held by the
  suspended Job while the popup is open; after an Activity recreation such a retry
  fails into a new popup. Intent events (clicks, submits) stay before `safeLaunch`
  so Retry does not repeat them; `trackOperation` inside the block reports each
  attempt.
- **`ErrorRecovery.Close(onClose)`** when a remote create may already have succeeded
  before a later step of the same block fails, so another attempt could duplicate
  it. The popup shows only Got it, and the required `onClose` runs after it closes.
  Current owners: TrainingCompleted save and ProfileCompleted profile creation
  (`onClose` navigates Back), Registration submit, ProfileBody apply and Debug
  training generation (`onClose = {}`).
- **Closing never re-runs the failed operation.** Do not start it again from
  `onClose`, and do not pass the enclosing function as `onClose`.
- **Cancellation while the popup is open.** Cancelling the Job cancels the pending
  decision. Feature code holds no `safeLaunch` Job, so this happens when the owning
  ViewModel is destroyed: its screen leaves the stack, or its dialog session is
  released. The popup stays on screen, but its buttons only close it (Retry still
  emits its intent event).
- **Host invariant.** The dialog host's own openings collection (`DialogViewModel`)
  is a pure state update and must not throw: a failure there would wait for a popup
  that only this collection can open.

Session-token observations (the `authorizationFeature.getToken()` chains in
`RootViewModel` and `ProfileCreationViewModel`) are long-lived routing sources on the
shared pipeline. By the owner's decision they restart only through Retry: after a closed
popup, token-loss navigation stays inactive until the owning ViewModel is recreated,
which for Root means a process restart.
Destinations follow [navigation ownership](navigation.md#direction--navigation-translation).

While a bottom sheet is open, the host shows the error above its active step, so a
dialog ViewModel and its suspended Job stay alive for Retry; see
[dialogs](dialogs.md#dialogcontroller--dialogprovider).
Error delivery and dismissal callbacks follow the
[dialog host retention policy](dialogs.md#dialogcontroller--dialogprovider); a later opening must
not discard the pending error whose callbacks complete this decision.

File logging isolation belongs to the
[logger sink](../../platform-build-toolkit/references/toolkit-utilities-core.md).

## Anti-patterns (MUST avoid)

- **Local catches that hide or present failures** — call throwing commands inside the shared pipeline, including private suspend helpers; requests driven by a latest-value source follow the source contract above.
- **Retrying from `onClose`** — closing the popup must not start the failed operation again; Retry is the button.
- **Converting a required credential failure into an absent account or early-success return.**
  Call the adapter directly inside the existing owned error/analytics block.
- **Adding Flow<Result<T>> for ordinary cached observations** instead of preserving Flow<Domain>. Observation failures still propagate through Flow.safeLaunch; cached data does not guarantee a successful stream.
- **Showing custom error dialogs** — use `DialogConfig.ErrorDisplay` via the pipeline.
- **Consuming `CancellationException`** — it must propagate; the shared dispatcher excludes it.
- **Recording an exception to Firebase manually** from a ViewModel — the pipeline does this; duplicates inflate Crashlytics.
- **Throwing a replacement `AppError` in a ViewModel** — use pure `safeLaunch.mapError` for necessary UI presentation while preserving the original error for analytics/logging/crash reporting.

Data-layer classification and payload completeness follow the
[canonical ownership rules](../../data-layer/references/operation-outcomes.md#operation-outcomes-and-cache-reconciliation-canonical).

## The error triad (`:ui-core:state` work)

Adding a new error type touches three places **atomically**:
1. `AppError.<New>` — the sealed hierarchy in `:ui-core:error:error-provider`.
2. `AppErrorState.<New>` — the UI mirror in `:ui-core:state` (package `core.state.error`).
3. The `when` branch in `ErrorProviderImpl.resolve(...)` mapping `AppError.<New>` → `AppErrorState.<New>`.

Unknown exceptions fall through as `AppErrorState.Unknown` (the `else` branch).
