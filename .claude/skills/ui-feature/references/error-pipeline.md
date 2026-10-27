# Error Pipeline

The `MyLoader`/`MyDirection`/`feature` examples are schematic; linked source files own real APIs.

Unhandled errors flow through a **single path**. Analytics observes the original
operation error without classifying it or changing recovery. Reporting respects core collection policy; ErrorProvider
translates failures into UI-friendly dialogs.

## The default pipeline

```
throw inside safeLaunch
  → operationManager catches via CoroutineExceptionHandler
  → BaseViewModel.sendError(exception, recovery, relaunch, mapError)
  → AppLogger.General.error(...)                     // file log
  → CrashReporting.reportException(...)              // toolkit core collection policy
  → ErrorProvider.provide(mapError(exception), onRetry, onClose = recovery.onClose) // UI presentation only
  → ErrorProviderImpl maps AppError → AppErrorState
  → DialogController.open(DialogConfig.ErrorDisplay(title, state, onRetry, onClose = recovery.onClose))
```

`relaunch` repeats the same `safeLaunch` call with identical arguments. `onRetry` is
that relaunch for `ErrorRecovery.Retry` (the default) and `null` for
`ErrorRecovery.Close`.

Every unhandled failure follows this path automatically. The ViewModel author writes only:

```kotlin
safeLaunch(loader = MyLoader.Fetch) {
    feature.fetch()
}
```

…and an error dialog with a Retry button appears if it fails. No `try/catch`, no
manual dialog dispatching, no hand-written retry.

## `ErrorProvider` and the dialog mapping

Inspect the current [ErrorProviderImpl](../../../../ui-core/error/error-provider-impl/src/commonMain/kotlin/com/grippo/core/error/provider/impl/ErrorProviderImpl.kt) for exact mapping and callback fields.

`AppErrorState` is the UI mirror of `AppError`. It lives in `:ui-core:state` (package `core.state.error`) — **not** in `:ui-core:error:error-provider`, which is kept UI-free so `:data-services:*` and `:toolkit:http-client` can depend on `AppError` without dragging in UI. Inspect `AppErrorState.kt`; current shape: `Network.Expected(title, description?)`, `data object`s `Network.NoInternet`, `Network.Timeout`, `Network.IncompleteResponse` and `Network.Unexpected`, a top-level `Expected(title, description?)` declared `: Network`, and `data object Unknown`. Fields are plain `String`s; `title()`/`description()` build the dialog's `UiText`, and the four transport objects take both from localized resources, so an exception's English message never reaches the dialog. On the `AppError` side, `Network.IncompleteResponse` and `Unknown` are no-argument plain classes; the other subtypes are data classes. `ErrorDisplay` is a `DialogConfig` subtype in `:ui-dialog-features:dialog-api`.

## Inside `BaseViewModel` — `sendError`

Inspect [BaseViewModel](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/BaseViewModel.kt)
and [OperationManagerImpl](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/internal/operation/OperationManagerImpl.kt).
Current shape: `OperationManagerImpl.launch` installs a `CoroutineExceptionHandler`
that ignores `CancellationException` and re-launches `onError(t)` on the VM scope;
that `onError` is `BaseViewModel.sendError` (`private suspend`), which runs
`AppLogger.General.error(...)` → `crashReporting.reportException(exception)` →
`displayError = if (exception is Exception) mapError(exception) else exception` →
`errorProvider.provide(displayError, onRetry, onClose = recovery.onClose)`. Original failures are
logged and reported; only `Exception` receives the pure `mapError` for the dialog,
and a fatal `Error` is not transformed. `recovery.onClose` runs after the popup
closes without Retry (Back, swipe, outside tap, toolbar Close or the Got-it button
of an `ErrorRecovery.Close` popup); Retry runs the relaunch instead. Neither is
immediate recovery.

Keep the error contract unified across block and Flow `safeLaunch`: both expose
`recovery: ErrorRecovery = ErrorRecovery.Retry()` (inspect
`ui-core/foundation/.../models/ErrorRecovery.kt`: `Retry(onClose)` offers Retry,
`Close(onClose)` offers only Got it) and `mapError: (Throwable) -> Throwable = { it }`,
with the same names, defaults and shared error pipeline. Pass them only when the
operation needs them; otherwise use the defaults.
`mapError` defaults to returning the original error. Use it only when a concrete boundary
exception needs an existing `AppError` representation for the standard dialog.
Keep it pure and non-throwing: no requests, state changes, navigation, reporting,
retries or business fallback. Return unrecognized errors unchanged. Analytics,
logging and crash reporting must receive the original exception; only
`ErrorProvider` receives the presentation mapping.

## Authoring rules (MUST)

```kotlin
// Good — errors flow through the pipeline; onClose cleans up when the popup is closed without Retry
safeLaunch(
    loader = MyLoader.SavingForm,
    recovery = ErrorRecovery.Retry(onClose = {
        update { it.copy(isSubmitted = false) }
    }),
) {
    val body = state.value.form.toDomain()
    feature.save(body)
    navigateTo(MyDirection.Close)
}
```

```kotlin
// BAD — manual catch bypasses the shared error path
safeLaunch {
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
`mapError` only for dialog presentation and `recovery.onClose` only after a close
without Retry.
Foundation's loader cleanup via `try/finally` and documented infrastructure boundaries
remain valid. Business contracts follow
[throwing suspend operations](../../data-layer/references/operation-outcomes.md#throwing-suspend-contracts-canonical).

## Requests driven by a long-lived source

An exception in `mapLatest` terminates its collection; only the popup's Retry
re-collects it, and closing leaves it terminated. When a period/filter observation must accept
later user choices after one request fails, keep the request in its own existing
`safeLaunch` error boundary and await it from the latest-value handler. If that
VM-owned job outlives a replaced value, cancel and join it in `finally` before
releasing the loader; noncancellable context is limited to that cleanup. Keep
the Job local to this operation, without a member, retry loop, stage flag or
new coroutine scope. This is failure isolation, not a recovery wrapper. A Retry of
such a request is a new VM-scoped job outside that boundary: it runs for its captured
value until it ends or the ViewModel is destroyed, a replaced value does not cancel
it, and if it overlaps the next request their shared loader clears when the first one
ends. This is accepted; do not add a member Job to own the retried request.

Before adding this boundary, trace the actual source and failure contract;
ordinary suspend helpers still propagate to their caller's `safeLaunch`.
Before removing it, check first failure -> next selection -> new request, rapid
replacement/cancellation and loader cleanup. Do not simplify a live subscription
into a pipeline that stops permanently after its first handled request error.

## Splash profile loading

Inspect `ui-screen-features/authorization/.../splash/SplashViewModel.kt`.
Its startup `safeLaunch` with `AppContent` loads the reference catalogs and calls
`resolveDirection()` in that same coroutine. Onboarding and token presence select
Onboarding/AuthProcess first; only an existing session reaches `hasProfile()`.

`hasProfile()` decorates its cached-user read and required remote `getUser()` with
SessionCheck. Its Boolean selects Home or ProfileCreation. Lookup failures use the
shared log/crash/dialog pipeline and do not become an absent profile. Retry
relaunches the complete startup sequence. Closing the popup continues through
`ErrorRecovery.Retry(onClose = ::openFromCache)`: `resolveCachedDirection()` uses only local data
(onboarding flag, token, cached user) without SessionCheck; a session without a
cached user goes to AuthProcess, never to ProfileCreation. If that local read fails
too, its Retry repeats it and closing goes to AuthProcess. Session-loss navigation
follows its [owning session observer](navigation.md#direction--navigation-translation).

## Retry and close

Retry is the user's choice, never automatic. The popup's Retry button relaunches the
failed `safeLaunch` call with identical arguments in the same ViewModel scope: the
block overload re-runs the same lambda with the same loader and callbacks, the Flow
overload re-collects the same flow instance. There is no retry loop, backoff or
analytics retry policy in the ViewModel; infrastructure retries with attempt limits
stay in their owning layer (for example the backend TokenProvider).

- **Retryable by default.** The safeLaunch block is the retried operation. Values
  captured before `safeLaunch` are reused; read state inside the block when a retry
  must see current state. Captured UI-bound handles (Login's Google/Apple UI context,
  the Completed share capture) stay alive while the popup is open; after an Activity
  recreation such a retry fails into a new popup. Intent events (clicks, submits) stay before `safeLaunch`
  so Retry does not repeat them; `trackOperation` inside the block reports each
  attempt.
- **`ErrorRecovery.Close`** when a remote create may already have succeeded before a
  later step of the same block fails, so a relaunch could duplicate it. The popup then
  shows Got it. Current owners: TrainingCompleted save, Registration submit,
  ProfileCompleted profile creation, ProfileBody apply and Debug training generation.
- **Closing never re-runs the failed operation.** `onClose` continues the flow
  instead: navigation (TrainingCompleted and ProfileCompleted go Back), cleanup
  (TrainingExercise validation) or a local fallback (Splash `openFromCache`,
  Onboarding consent observation and `continueWithoutConsent`). Do not pass the
  enclosing function as `onClose`.
- A retry after the owning ViewModel is destroyed is a no-op on its cancelled scope.

Session-token observations (the `authorizationFeature.getToken()` chains in
`RootViewModel` and `ProfileCreationViewModel`) are long-lived routing sources on the
shared pipeline. By the owner's decision they restart only through Retry: after a closed
popup, token-loss navigation stays inactive until the owning ViewModel is recreated,
which for Root means a process restart.
Destinations follow [navigation ownership](navigation.md#direction--navigation-translation).

While a bottom sheet is open, the host shows the error above its active step, so a
dialog ViewModel stays alive for Retry; see
[dialogs](dialogs.md#dialogcontroller--dialogprovider).
Error delivery and dismissal callbacks follow the
[dialog host retention policy](dialogs.md#dialogcontroller--dialogprovider); a later opening must
not discard the pending error that owns this recovery callback.

File logging isolation belongs to the
[logger sink](../../platform-build-toolkit/references/toolkit-utilities-core.md).

## Anti-patterns (MUST avoid)

- **Local catches that hide or present failures** — call throwing commands inside the shared pipeline, including private suspend helpers; request isolation follows the source contract above.
- **Retrying from `onClose`** — closing the popup must not relaunch the failed operation; Retry is the button.
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
3. The `when` branch in `ErrorProviderImpl.provide(...)` mapping `AppError.<New>` → `AppErrorState.<New>`.

Unknown exceptions fall through as `AppErrorState.Unknown` (the `else` branch).
