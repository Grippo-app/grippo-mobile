# Error Pipeline

Unhandled errors flow through a **single path**. UI analytics bindings may classify typed exceptions without replacing the
existing error path. Reporting respects core collection policy; ErrorProvider
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

```kotlin
public interface ErrorProvider {
    public suspend fun provide(exception: Throwable, callback: () -> Unit)
}

@Single(binds = [ErrorProvider::class])
internal class ErrorProviderImpl(val dialogController: DialogController) : ErrorProvider {
    override suspend fun provide(exception: Throwable, callback: () -> Unit) {
        val state: AppErrorState = when (exception) {
            is AppError.Network.NoInternet -> AppErrorState.Network.NoInternet(description = exception.message)
            is AppError.Network.Timeout -> AppErrorState.Network.Timeout(description = exception.message)
            is AppError.Network.Expected -> AppErrorState.Network.Expected(title = exception.title, description = exception.description)
            is AppError.Network.Unexpected -> AppErrorState.Network.Unexpected(description = exception.message)
            is AppError.Expected -> AppErrorState.Expected(title = exception.message, description = exception.description)
            is AppError.Unknown -> AppErrorState.Unknown
            else -> AppErrorState.Unknown
        }
        dialogController.open(
            DialogConfig.ErrorDisplay(title = state.title(), error = state, onClose = callback)
        )
    }
}
```

`AppErrorState` is the UI mirror of `AppError`. It lives in `:ui-core:state` (package `core.state.error`) — **not** in `:ui-core:error:error-provider`, which is kept UI-free so `:data-services:*` and `:toolkit:http-client` can depend on `AppError` without dragging in UI. Constructor params are plain strings (`title: String`, `description: String?`); `UiText` wrapping is a per-product decision, not part of the bootstrap shape. `ErrorDisplay` is a `DialogConfig` subtype in `:ui-dialog-features:dialog-api`.

## Inside `BaseViewModel` — `sendError`

```kotlin
private val errorProvider by inject<ErrorProvider>()
private val crashReporting by inject<CrashReporting>()

private suspend fun sendError(
    exception: Throwable,
    onError: () -> Unit,
    mapError: (Throwable) -> Throwable,
) {
    val log = buildString {
        append("─────── ViewModel error ──────\n")
        append("│ message: ${exception.message}\n")
        append("│ cause: ${exception.cause?.message}\n")
        append("└──────────────────────────")
    }
    AppLogger.General.error(log)
    crashReporting.reportException(exception)
    val displayError = if (exception is Exception) {
        mapError(exception)
    } else {
        exception
    }
    errorProvider.provide(displayError, callback = onError)
}
```

The `CancellationException` filter lives one layer up, in `OperationManagerImpl`:

```kotlin
val handler = CoroutineExceptionHandler { _, t ->
    if (t !is CancellationException) {
        coroutineScope.launch {
            onError(t)
        }
    }
}
```

`CrashReporting` is injected from toolkit API; platform SDK adapters are attached
by app shells. Reporting follows core collection policy. Analytics classifiers
may inspect typed failures at the UI boundary, but preserve the original error
path. With the standard pipeline, `onError` runs after dismissal and can clean up UI state.
It is not an immediate exception handler and cannot suppress the dialog or
continue the failed business block. `CancellationException` never enters this
pipeline or `mapError`; fatal `Error` is not transformed for presentation.

Both `safeLaunch` overloads accept optional `mapError: (Throwable) -> Throwable`.
Its default returns the original error. Use it only when a concrete boundary
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
`Result.onSuccess` may consume successful data without hiding failure. Use pure
`mapError` only for dialog presentation and `onError` only after dismissal.
Foundation's loader cleanup via `try/finally` and service/domain error boundaries
remain valid.

## Explicit business fallback

When an existing business policy requires immediate fallback for one operation,
catch ordinary `Exception` around only that operation in the same coroutine.
Rethrow `CancellationException` first. Never catch `Throwable` or fatal `Error`,
or include unrelated requests, successful navigation or fallback execution in
that protected block. Their failures must still reach the standard pipeline.
Do not add launches, scopes, wrappers, stage flags or recovery extensions.
A private suspend method is sufficient when the operation has a meaningful name.
Keep recovery business-owned, outside analytics and presentation mapping.

Splash has one `safeLaunch` with `AppContent`: token/reference failures use the
standard pipeline and retry the complete sequence only after dismissal. With a
token, it calls this private suspend method in that same coroutine:

```kotlin
private suspend fun loadProfile() {
    val hasProfile = try {
        analytics.trackOperation(AuthOperation.SessionCheck) {
            hasProfile()
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        navigateTo(
            SplashDirection.AuthProcess(AuthorizationEntryPoint.SessionCheckFailed)
        )
        return
    }

    val destination = if (hasProfile) {
        SplashDirection.Home
    } else {
        SplashDirection.CreateProfile
    }
    navigateTo(destination)
}
```

The catch surrounds only the profile check, outside `trackOperation`: analytics
observes the original failure before fallback, rather than reporting a handled
failure as completed. `hasProfile` is a business Boolean. Successful navigation
and the fallback run outside the protected block, and cancellation/fatal errors
are not converted to authorization. No second launch or loader is needed.

## Only emits, not retries

The pipeline shows the error and runs `onError` after dialog dismissal. It does
not automatically retry or resume the failed block. Infrastructure retries with
backoff and attempt limits belong in the Repository (`TokenProvider.retryWithBackoff`).
A feature may explicitly continue or retry its business operation from `onError`
after the user dismisses the dialog. Relaunch the complete required sequence through
`safeLaunch` in the existing ViewModel scope; do not add a new scope, a retry loop
before dismissal or an analytics retry policy.

## Anti-patterns (MUST avoid)

- **Local catches that hide or present unhandled failures** — use `getOrThrow()` and the shared pipeline. Only a documented business fallback may use the narrowly scoped catch above; no arbitrary suppression or extra launch.
- **`Result.getOrNull()` in ViewModel** without handling `null` — silently drops failures.
- **Returning `Result<T>` from a Feature observation** — observation must always succeed (cached data); errors are values for mutations only.
- **Showing custom error dialogs** — use `DialogConfig.ErrorDisplay` via the pipeline.
- **Consuming `CancellationException`** — it must propagate; the shared dispatcher excludes it, and explicit fallback catches rethrow it first.
- **Recording an exception to Firebase manually** from a ViewModel — the pipeline does this; duplicates inflate Crashlytics.
- **Throwing a replacement `AppError` in a ViewModel** — use pure `safeLaunch.mapError` for necessary UI presentation while preserving the original error for analytics/logging/crash reporting.

## The error triad (`:ui-core:state` work)

Adding a new error type touches three places **atomically**:
1. `AppError.<New>` — the sealed hierarchy in `:ui-core:error:error-provider`.
2. `AppErrorState.<New>` — the UI mirror in `:ui-core:state` (package `core.state.error`).
3. The `when` branch in `ErrorProviderImpl.provide(...)` mapping `AppError.<New>` → `AppErrorState.<New>`.

Unknown exceptions fall through as `AppErrorState.Unknown` (the `else` branch).
