# Error Pipeline

All errors flow through a **single path**. UI analytics bindings may classify typed exceptions without replacing the
existing error path. Reporting respects core collection policy; ErrorProvider
translates failures into UI-friendly dialogs.

## The pipeline

```
throw inside safeLaunch
  → operationManager catches via CoroutineExceptionHandler
  → BaseViewModel.sendError(exception, onError)
  → AppLogger.General.error(...)                     // file log
  → CrashReporting.reportException(...)              // toolkit core collection policy
  → ErrorProvider.provide(exception, onError)
  → ErrorProviderImpl maps AppError → AppErrorState
  → DialogController.show(DialogConfig.ErrorDisplay(state, onClose = onError))
```

Every step is automatic. The ViewModel author writes only:

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
        dialogController.show(DialogConfig.ErrorDisplay(error = state, onClose = callback))
    }
}
```

`AppErrorState` is the UI mirror of `AppError`. It lives in `:ui-core:state` (package `core.state.error`) — **not** in `:ui-core:error:error-provider`, which is kept UI-free so `:data-services:*` and `:toolkit:http-client` can depend on `AppError` without dragging in UI. Constructor params are plain strings (`title: String`, `description: String?`); `UiText` wrapping is a per-product decision, not part of the bootstrap shape. `ErrorDisplay` is a `DialogConfig` subtype in `:ui-dialog-features:dialog-api`.

## Inside `BaseViewModel` — `sendError`

```kotlin
private val errorProvider by inject<ErrorProvider>()
    private val crashReporting by inject<CrashReporting>()

private suspend fun sendError(exception: Throwable, onError: (() -> Unit)) {
    val log = buildString {
        append("─────── ViewModel error ──────\n")
        append("│ message: ${exception.message}\n")
        append("│ cause: ${exception.cause?.message}\n")
        append("└──────────────────────────")
    }
    AppLogger.General.error(log)
    crashReporting.reportException(exception)
    errorProvider.provide(exception, callback = onError)
}
```

The `CancellationException` filter lives one layer up, in `OperationManagerImpl`:

```kotlin
val handler = CoroutineExceptionHandler { _, t ->
    if (t !is CancellationException) coroutineScope.launch { onError(t) }
}
```

`CrashReporting` is injected from toolkit API; platform SDK adapters are attached
by app shells. Reporting follows core collection policy. Analytics classifiers
may inspect typed failures at the UI boundary, but preserve the original error
path. The `onError` callback runs after dismissal and can clean up UI state.

## Authoring rules (MUST)

```kotlin
// Good — errors flow through the pipeline; onError cleans up after the dialog is dismissed
safeLaunch(loader = MyLoader.SavingForm, onError = { update { it.copy(isSubmitted = false) } }) {
    val body = state.value.form.toDomain()
    feature.save(body).getOrThrow()
    navigateTo(MyDirection.Close)
}
```

```kotlin
// BAD — manual catch swallows the error path (no Firebase, no dialog)
safeLaunch {
    try {
        feature.save(...).getOrThrow()
        navigateTo(...)
    } catch (e: Throwable) {
        update { it.copy(error = e.message) }    // forbidden
    }
}
```

The **only** legitimate `try/catch` inside a ViewModel is when you genuinely want to recover and **not** show an error to the user — rare, warrants a code comment explaining why. `Result.onSuccess { ... } / .onFailure { ... }` and `runCatching { ... }` at a domain boundary (collapsing a thrown exception into a `Result`) are the same carve-out expressed at the type level. Both carve-outs are the documented exceptions.

## Only emits, not retries

The pipeline **shows** the error and runs the optional `onError` callback. It does **not** auto-retry. Retries (with backoff, max attempts) belong inside the Repository for transient infrastructure errors (`TokenProvider.retryWithBackoff`) — **not** inside the ViewModel.

## Anti-patterns (MUST avoid)

- **`try/catch` in ViewModel** to "log and ignore" — bypasses Firebase, hides bugs.
- **`Result.getOrNull()` in ViewModel** without handling `null` — silently drops failures.
- **Returning `Result<T>` from a Feature observation** — observation must always succeed (cached data); errors are values for mutations only.
- **Showing custom error dialogs** — use `DialogConfig.ErrorDisplay` via the pipeline.
- **Catching `CancellationException`** — never; it must propagate to cancel parent jobs.
- **Recording an exception to Firebase manually** from a ViewModel — the pipeline does this; duplicates inflate Crashlytics.
- **Using `AppError.Expected` from a UI module** — only domain code throws `AppError`; UI surfaces it via the pipeline.

## The error triad (`:ui-core:state` work)

Adding a new error type touches three places **atomically**:
1. `AppError.<New>` — the sealed hierarchy in `:ui-core:error:error-provider`.
2. `AppErrorState.<New>` — the UI mirror in `:ui-core:state` (package `core.state.error`).
3. The `when` branch in `ErrorProviderImpl.provide(...)` mapping `AppError.<New>` → `AppErrorState.<New>`.

Unknown exceptions fall through as `AppErrorState.Unknown` (the `else` branch).
