# Error Pipeline

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
    feature.fetch().getOrThrow()        // .getOrThrow() converts Result.Failure into a throw
}
```

…and an error dialog appears if it fails. No `try/catch`, no manual dialog dispatching.

## `ErrorProvider` and the dialog mapping

Inspect the current [ErrorProviderImpl](../../../../ui-core/error/error-provider-impl/src/commonMain/kotlin/com/grippo/core/error/provider/impl/ErrorProviderImpl.kt) for exact mapping and callback fields.

`AppErrorState` is the UI mirror of `AppError`. It lives in `:ui-core:state` (package `core.state.error`) — **not** in `:ui-core:error:error-provider`, which is kept UI-free so `:data-services:*` and `:toolkit:http-client` can depend on `AppError` without dragging in UI. Constructor params are plain strings (`title: String`, `description: String?`); `UiText` wrapping is a per-product decision, not part of the bootstrap shape. `ErrorDisplay` is a `DialogConfig` subtype in `:ui-dialog-features:dialog-api`.

## Inside `BaseViewModel` — `sendError`

Inspect [BaseViewModel](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/BaseViewModel.kt)
and [OperationManagerImpl](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/internal/operation/OperationManagerImpl.kt).
CancellationException is excluded before reporting. Original failures are logged
and reported; only Exception receives pure mapError for the dialog. Fatal Error is
not transformed. onError is a post-dismissal callback, not immediate recovery.

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
    feature.save(body).getOrThrow()
    navigateTo(MyDirection.Close)
}
```

```kotlin
// BAD — manual catch bypasses the shared error path
safeLaunch {
    try {
        feature.save(...).getOrThrow()
        navigateTo(...)
    } catch (e: Throwable) {
        update { it.copy(error = e.message) }    // forbidden
    }
}
```

Do not hide, replace or present unhandled failures through local catches,
`runCatching`, `Result.onFailure`, `recover`, `fold` or `getOrElse` in a ViewModel
or its helpers. Unwrap failed results with `getOrThrow()` inside `safeLaunch`.
`Result.onSuccess` alone does not propagate failures. Unwrap the returned Result
with `getOrThrow()` inside `safeLaunch` before treating the operation as complete. Use pure
`mapError` only for dialog presentation and `onError` only after dismissal.
Foundation's loader cleanup via `try/finally` and service/domain error boundaries
remain valid.

## Splash profile loading

Splash has one `safeLaunch` with `AppContent`, started through class-level
`loadContent()` from `init` and reused by `onError = ::loadContent`. Token,
reference and profile failures all use the standard log/crash/dialog pipeline.
Closing the dialog retries the complete sequence; Splash itself does not turn
lookup failures into authorization or Home navigation. Root independently owns
navigation after token loss. With a token, `loadProfile()` runs in that same coroutine:

```kotlin
private suspend fun loadProfile() {
    val hasProfile = analytics.trackOperation(AuthOperation.SessionCheck) {
        hasProfile()
    }

    val destination = if (hasProfile) {
        SplashDirection.Home
    } else {
        SplashDirection.CreateProfile
    }
    navigateTo(destination)
}
```

`hasProfile` is a business Boolean: `true` leads to Home, `false` to CreateProfile.
Analytics decorates only the lookup
and preserves its result/error. Cancellation propagates naturally; there is no
local catch, second launch, extra loader or recovery wrapper.

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

- **Local catches that hide or present failures** — use `getOrThrow()` and the shared pipeline, including private suspend helpers; no suppression or extra launch.
- **`Result.getOrNull()` in ViewModel** without handling `null` — silently drops failures.
- **Adding Flow<Result<T>> for ordinary cached observations** instead of preserving Flow<Domain>. Observation failures still propagate through Flow.safeLaunch; cached data does not guarantee a successful stream.
- **Showing custom error dialogs** — use `DialogConfig.ErrorDisplay` via the pipeline.
- **Consuming `CancellationException`** — it must propagate; the shared dispatcher excludes it.
- **Recording an exception to Firebase manually** from a ViewModel — the pipeline does this; duplicates inflate Crashlytics.
- **Throwing a replacement `AppError` in a ViewModel** — use pure `safeLaunch.mapError` for necessary UI presentation while preserving the original error for analytics/logging/crash reporting.

Data-layer classification and payload completeness follow the
[canonical ownership rules](../../data-layer/references/error-pipeline.md).

## The error triad (`:ui-core:state` work)

Adding a new error type touches three places **atomically**:
1. `AppError.<New>` — the sealed hierarchy in `:ui-core:error:error-provider`.
2. `AppErrorState.<New>` — the UI mirror in `:ui-core:state` (package `core.state.error`).
3. The `when` branch in `ErrorProviderImpl.provide(...)` mapping `AppError.<New>` → `AppErrorState.<New>`.

Unknown exceptions fall through as `AppErrorState.Unknown` (the `else` branch).
