# Error Pipeline

The `MyLoader`/`MyDirection`/`feature` examples are schematic; linked source files own real APIs.

Unhandled errors flow through a **single path**. Analytics observes the original
operation error without classifying it or changing recovery. Reporting respects core collection policy; ErrorProvider
translates failures into UI-friendly dialogs.

## The default pipeline

```
throw inside safeLaunch
  → operationManager catches via CoroutineExceptionHandler
  → BaseViewModel.sendError(exception, onError, mapError)
  → AppLogger.General.error(...)                     // file log
  → CrashReporting.reportException(...)              // toolkit core collection policy
  → ErrorProvider.provide(mapError(exception), onError) // UI presentation only
  → ErrorProviderImpl maps AppError → AppErrorState
  → DialogController.open(DialogConfig.ErrorDisplay(title, state, onClose = onError))
```

Every unhandled failure follows this path automatically. The ViewModel author writes only:

```kotlin
safeLaunch(loader = MyLoader.Fetch) {
    feature.fetch()
}
```

…and an error dialog appears if it fails. No `try/catch`, no manual dialog dispatching.

## `ErrorProvider` and the dialog mapping

Inspect the current [ErrorProviderImpl](../../../../ui-core/error/error-provider-impl/src/commonMain/kotlin/com/grippo/core/error/provider/impl/ErrorProviderImpl.kt) for exact mapping and callback fields.

`AppErrorState` is the UI mirror of `AppError`. It lives in `:ui-core:state` (package `core.state.error`) — **not** in `:ui-core:error:error-provider`, which is kept UI-free so `:data-services:*` and `:toolkit:http-client` can depend on `AppError` without dragging in UI. Inspect `AppErrorState.kt`; current shape: `Network.Expected(title, description?)`, `Network.NoInternet(description?)`, `Network.Timeout(description?)`, `Network.IncompleteResponse(description?)`, `Network.Unexpected(description?)`, a top-level `Expected(title, description?)` declared `: Network`, and `data object Unknown`. Fields are plain `String`s; `title()`/`description()` build the dialog's `UiText`. On the `AppError` side, `Network.IncompleteResponse` and `Unknown` are no-argument plain classes; the other subtypes are data classes. `ErrorDisplay` is a `DialogConfig` subtype in `:ui-dialog-features:dialog-api`.

## Inside `BaseViewModel` — `sendError`

Inspect [BaseViewModel](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/BaseViewModel.kt)
and [OperationManagerImpl](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/internal/operation/OperationManagerImpl.kt).
Current shape: `OperationManagerImpl.launch` installs a `CoroutineExceptionHandler`
that ignores `CancellationException` and re-launches `onError(t)` on the VM scope;
that `onError` is `BaseViewModel.sendError` (`private suspend`), which runs
`AppLogger.General.error(...)` → `crashReporting.reportException(exception)` →
`displayError = if (exception is Exception) mapError(exception) else exception` →
`errorProvider.provide(displayError, callback = onError)`. Original failures are
logged and reported; only `Exception` receives the pure `mapError` for the dialog,
and a fatal `Error` is not transformed. `onError` is a post-dismissal callback,
not immediate recovery.

Keep the error contract unified across block and Flow `safeLaunch`: both expose
`onError: () -> Unit = {}` and `mapError: (Throwable) -> Throwable = { it }`, with
the same names, defaults and shared error pipeline. Pass either callback only
when the operation needs it; otherwise use the defaults.
`mapError` defaults to returning the original error. Use it only when a concrete boundary
exception needs an existing `AppError` representation for the standard dialog.
Keep it pure and non-throwing: no requests, state changes, navigation, reporting,
retries or business fallback. Return unrecognized errors unchanged. Analytics,
logging and crash reporting must receive the original exception; only
`ErrorProvider` receives the presentation mapping.

## Authoring rules (MUST)

```kotlin
// Good — errors flow through the pipeline; onError cleans up after the dialog is dismissed
safeLaunch(
    loader = MyLoader.SavingForm,
    onError = {
        update { it.copy(isSubmitted = false) }
    },
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
Keep all required work inside the decorated business block. Credential adapters that return
`Result` still need `getOrThrow()` inside that block; ignoring a failed Result loses its error. Use pure
`mapError` only for dialog presentation and `onError` only after dismissal.
Foundation's loader cleanup via `try/finally` and documented infrastructure boundaries
remain valid. Business contracts follow
[throwing suspend operations](../../data-layer/references/operation-outcomes.md#throwing-suspend-contracts-canonical).

## Requests driven by a long-lived source

An exception in `mapLatest` terminates its collection; the shared error dialog
does not restart that subscription. When a period/filter observation must accept
later user choices after one request fails, keep the request in its own existing
`safeLaunch` error boundary and await it from the latest-value handler. If that
VM-owned job outlives a replaced value, cancel and join it in `finally` before
releasing the loader; noncancellable context is limited to that cleanup. Keep
the Job local to this operation, without a member, retry loop, stage flag or
new coroutine scope. This is failure isolation, not a recovery wrapper.

Before adding this boundary, trace the actual source and failure contract;
ordinary suspend helpers still propagate to their caller's `safeLaunch`.
Before removing it, check first failure -> next selection -> new request, rapid
replacement/cancellation and loader cleanup. Do not simplify a live subscription
into a pipeline that stops permanently after its first handled request error.

## Splash profile loading

Inspect `ui-screen-features/authorization/.../splash/SplashViewModel.kt`.
`loadContent()` starts one `safeLaunch` with `AppContent`, reused by
`onError = ::loadContent`. It loads the reference catalogs and calls
`resolveDirection()` in that same coroutine. Onboarding and token presence select
Onboarding/AuthProcess first; only an existing session reaches `hasProfile()`.

`hasProfile()` decorates its cached-user read and required remote `getUser()` with
SessionCheck. Its Boolean selects Home or ProfileCreation. Lookup failures use the
shared log/crash/dialog pipeline and do not become an absent profile. Closing the
error dialog retries the complete startup sequence; Root independently owns
navigation after token loss. No recovery wrapper or second launch is needed.

## Only emits, not retries

The pipeline shows the error and runs `onError` after dialog dismissal. It does
not automatically retry or resume the failed block. Infrastructure retries with
backoff and attempt limits stay in their existing owning layer (for example the
backend TokenProvider), rather than becoming a ViewModel recovery loop.
A feature may explicitly continue or retry its business operation from `onError`
after the user dismisses the dialog. Relaunch the complete required sequence through
`safeLaunch` in the existing ViewModel scope; do not add a new scope, a retry loop
before dismissal or an analytics retry policy.

## Anti-patterns (MUST avoid)

- **Local catches that hide or present failures** — call throwing commands inside the shared pipeline, including private suspend helpers; request isolation follows the source contract above.
- **Discarding a required credential Result failure through `getOrNull()`, `getOrElse`
  or an early-success return.** Unwrap it with `getOrThrow()` inside the existing owned
  error/analytics block; handling the fallback value does not preserve the failure.
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
