# Error Pipeline

All errors flow through a **single path**. The UI never inspects exception types directly — by the time an error reaches the user, it has been logged, reported to Crashlytics, and translated into a UI-friendly dialog.

## The pipeline

```
throw inside safeLaunch
       ↓
operationManager catches via CoroutineExceptionHandler
       ↓
BaseViewModel.sendError(exception, onError)
       ↓
AppLogger.General.error(...)                          // file log
FirebaseProvider.recordException(...)                 // crashlytics
ErrorProvider.provide(exception, onError)
       ↓
ErrorProviderImpl maps AppError → AppErrorState
       ↓
DialogController.show(DialogConfig.ErrorDisplay(state, onClose = onError))
```

Every step is automatic. The ViewModel author writes:

```kotlin
safeLaunch(loader = MyLoader.Fetch) {
    feature.fetch().getOrThrow()        // .getOrThrow() converts Result.Failure into a throw
}
```

…and an error dialog appears if the network fails. No `try/catch`, no manual dialog dispatching.

## Where errors originate

### Network: `BackendClient` → `ApiErrorParser` → `AppError`

The Ktor `HttpClient` is configured with a `HttpResponseValidator`:

```kotlin
// :toolkit:http-client
HttpResponseValidator {
    validateResponse { response ->
        if (response.status.isSuccess()) return@validateResponse
        throw ApiErrorParser.parse(response)
    }
}
```

`ApiErrorParser.parse(response)` returns an `AppError` subtype:

- `HttpStatusCode.RequestTimeout` (408) → `AppError.Network.Timeout`
- No internet (transport-level) → `AppError.Network.NoInternet`
- 4xx with a parseable body → `AppError.Network.Expected(keys, title, description)`
- 4xx/5xx without a useful body → `AppError.Network.Unexpected(statusCode, message)`

### Domain: explicit `throw AppError.Expected(...)`

A use case may decide that a domain rule failed (e.g. "cannot delete the last training of an active goal") and throw:

```kotlin
throw AppError.Expected(
    message = "Cannot delete this training",
    description = "Your active goal would lose its anchor.",
)
```

### Unknown: any other `Throwable`

Falls through as `AppError.Unknown` after `ErrorProviderImpl` defaults the mapping.

## `AppError` hierarchy

```kotlin
public sealed class AppError(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    public sealed class Network(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppError(message, cause) {

        public data class Expected(
            val keys: List<String> = emptyList(),
            val title: String,
            val description: String?,
            override val cause: Throwable? = null,
        ) : Network(title, cause)

        public data class NoInternet(
            override val message: String,
            override val cause: Throwable? = null,
        ) : Network(message, cause)

        public data class Timeout(
            override val message: String,
            override val cause: Throwable? = null,
        ) : Network(message, cause)

        public data class Unexpected(
            val statusCode: Int? = null,
            override val message: String,
            override val cause: Throwable? = null,
        ) : Network(message, cause)
    }

    public data class Expected(
        override val message: String,
        val description: String?,
    ) : AppError(null)

    public class Unknown : AppError(null)
}
```

## `ErrorProvider` and the dialog mapping

```kotlin
public interface ErrorProvider {
    public suspend fun provide(exception: Throwable, callback: () -> Unit)
}

@Single(binds = [ErrorProvider::class])
internal class ErrorProviderImpl(
    val dialogController: DialogController,
) : ErrorProvider {

    override suspend fun provide(exception: Throwable, callback: () -> Unit) {
        val state: AppErrorState = when (exception) {
            is AppError.Network.NoInternet ->
                AppErrorState.Network.NoInternet(description = exception.message)
            is AppError.Network.Timeout ->
                AppErrorState.Network.Timeout(description = exception.message)
            is AppError.Network.Expected ->
                AppErrorState.Network.Expected(title = exception.title, description = exception.description)
            is AppError.Network.Unexpected ->
                AppErrorState.Network.Unexpected(description = exception.message)
            is AppError.Expected ->
                AppErrorState.Expected(title = exception.message, description = exception.description)
            is AppError.Unknown -> AppErrorState.Unknown
            else -> AppErrorState.Unknown
        }
        dialogController.show(DialogConfig.ErrorDisplay(error = state, onClose = callback))
    }
}
```

`AppErrorState` is the UI mirror of `AppError`, with `UiText`-friendly fields. It lives in `:ui-core:error:error-provider`. `ErrorDisplay` is a `DialogConfig` subtype in `:ui-dialog-features:dialog-api`.

## Inside `BaseViewModel`

The viewmodel has a private `suspend` `sendError` invoked from the `onError` lambda that `OperationManager` passes to its `CoroutineExceptionHandler`:

```kotlin
private val errorProvider by inject<ErrorProvider>()

private suspend fun sendError(exception: Throwable, onError: (() -> Unit)) {
    val log = buildString {
        append("─────── ViewModel error ──────\n")
        append("│ message: ${exception.message}\n")
        append("│ cause: ${exception.cause?.message}\n")
        append("└──────────────────────────")
    }
    AppLogger.General.error(log)
    FirebaseProvider.recordException(exception)
    errorProvider.provide(exception, callback = onError)
}
```

The `CancellationException` filter lives one layer up, in `OperationManagerImpl`:

```kotlin
val handler = CoroutineExceptionHandler { _, t ->
    if (t !is CancellationException) coroutineScope.launch { onError(t) }
}
```

`FirebaseProvider` is the toolkit object (its `recordException` is a top-level static call), not a Koin-injected dependency. The `onError` lambda is provided by the call site (the `onError = { ... }` parameter of `safeLaunch`). It runs **after** the user dismisses the error dialog — useful for state cleanup ("reset the form on error").

## Authoring rules

### Inside a ViewModel

```kotlin
// Good — errors flow through the pipeline
safeLaunch(loader = MyLoader.SavingForm, onError = { update { it.copy(isSubmitted = false) } }) {
    val body = state.value.form.toDomain()
    feature.save(body).getOrThrow()
    navigateTo(MyDirection.Close)
}
```

```kotlin
// BAD — manual catch swallows the error path
safeLaunch {
    try {
        feature.save(...).getOrThrow()
        navigateTo(...)
    } catch (e: Throwable) {
        update { it.copy(error = e.message) }    // forbidden — no Firebase, no dialog
    }
}
```

The **only** legitimate `try/catch` inside a ViewModel is when you genuinely want to recover and **not** show an error to the user. Such cases are rare and warrant a code comment explaining why.

### Inside a Repository

```kotlin
// Good — runCatching converts to Result
override suspend fun saveTraining(training: Training): Result<String> = runCatching {
    val body = training.toBody()
    val response = api.setTraining(body).getOrThrow()
    val id = response.id ?: error("missing id")
    trainingDao.insert(training.toEntity(id))
    id
}
```

```kotlin
// Also good — explicit Result map/onSuccess chain (when partial success matters)
override suspend fun saveTraining(training: Training): Result<String> {
    val response = api.setTraining(training.toBody())
    return response.mapCatching { dto ->
        val id = dto.id ?: error("missing id")
        api.getTraining(id).onSuccess { full ->
            full.toEntityOrNull()?.let { trainingDao.insert(it) }
        }
        id
    }
}
```

### Inside a UseCase

```kotlin
@Factory
public class RecalculateGoalProgressUseCase(
    private val goalFeature: GoalFeature,
    private val trainingsFeature: TrainingsFeature,
) {
    public suspend fun execute(goalId: String): Result<Unit> = runCatching {
        val goal = goalFeature.getGoal(goalId).getOrThrow()
        val trainings = trainingsFeature.getTrainingsForRange(goal.range).getOrThrow()
        val progress = computeProgress(goal, trainings)
        goalFeature.updateProgress(goalId, progress).getOrThrow()
    }
}
```

`getOrThrow()` inside `runCatching { ... }` is the idiomatic pattern: the `Result.Failure` is unwrapped, then re-wrapped by the outer `runCatching` — no double-handling.

## Only emits, not retries

The pipeline **shows** the error and runs the optional `onError` callback. It does **not** auto-retry. Retries (with backoff, max attempts) belong inside the Repository for transient infrastructure errors (see `TokenProvider.retryWithBackoff`) — not inside the ViewModel.

## Anti-patterns

- **`try/catch` in ViewModel** to "log and ignore" — bypasses Firebase, hides bugs.
- **`Result.getOrNull()` in ViewModel** without handling `null` — silently drops failures.
- **Returning `Result<T>` from a Feature observation** — observation is for cached data; errors are values for mutations, but observations must always succeed.
- **Showing custom error dialogs** — use `DialogConfig.ErrorDisplay` via the pipeline.
- **Catching `CancellationException`** — never; it must propagate to cancel parent jobs.
- **Recording an exception to Firebase manually** from a ViewModel — the pipeline does this. Duplicate reports inflate Crashlytics.
- **Using `AppError.Expected` from a UI module** — only domain code throws `AppError`; UI surfaces it via the pipeline.
