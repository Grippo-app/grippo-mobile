# Error pipeline — the data-layer view

Self-contained reference for the data-layer-relevant slices of the error pipeline: where errors
originate (network validator, domain, unknown), the `AppError` hierarchy, and the Repository /
UseCase authoring rules. The UI-side dialog mapping is covered by the presentation-layer skill.

> **Illustrative domain.** Code uses `Note` / `Tag` / `User` as the generic `<Entity>` /
> `<RelatedEntity>`. Substitute identifiers from your product domain.

---

## Where errors originate — Network validator (EXAMPLE)

The Ktor `HttpClient` is configured via the `responseValidator(apiErrorParser)` extension in
`:toolkit:http-client`. It installs Ktor's `HttpResponseValidator` with two blocks — one for HTTP
status codes, one for transport-level exceptions:

```kotlin
// :toolkit:http-client/internal/ResponseValidator.kt
internal fun HttpClientConfig<*>.responseValidator(
    apiErrorParser: ApiErrorParser,
) = HttpResponseValidator {
    validateResponse { response ->
        val statusCode = response.status.value
        if (statusCode in 200..299) return@validateResponse

        val rawBody = try {
            response.bodyAsText()
        } catch (cause: CancellationException) {
            throw cause
        } catch (_: IOException) {
            null
        }
        when (statusCode) {
            in 400..499 -> {
                val parsed = apiErrorParser.parseDetailedMessage(rawBody, statusCode)
                throw AppError.Network.Expected(
                    keys = apiErrorParser.parseKeys(rawBody),
                    title = parsed.title,
                    description = parsed.description,
                )
            }
            in 500..599 -> throw AppError.Network.Unexpected(
                message = apiErrorParser.getDefaultServerErrorMessage(statusCode),
            )
            else -> throw AppError.Network.Unexpected(message = "Unexpected HTTP code: $statusCode")
        }
    }

    handleResponseExceptionWithRequest { cause, _ ->
        if (cause !is Exception) {
            throw cause
        }

        when (cause) {
            is CancellationException -> throw cause
            is AppError -> throw cause

            is HttpRequestTimeoutException -> throw AppError.Network.Timeout(message = "Request timed out. Try again.", cause = cause)
            is JsonConvertException -> throw AppError.Network.Unexpected(message = "Invalid server response format.", cause = cause)
            is IOException -> throw AppError.Network.NoInternet(message = "Connection lost or unavailable.", cause = cause)
            else -> throw AppError.Network.Unexpected(message = cause.message ?: "Unexpected network error", cause = cause)
        }
    }
}
```

The mapping the validator produces:

- Caller `CancellationException` (including `TimeoutCancellationException`) and
  fatal `Error`/other non-Exception Throwables → rethrow unchanged; existing
  `AppError` → rethrow unchanged.
- `HttpRequestTimeoutException` → `AppError.Network.Timeout`; do not classify every
  coroutine timeout as a request timeout.
- `IOException` (transport-level no internet) → `AppError.Network.NoInternet`
- 4xx response with a parseable body → `AppError.Network.Expected(keys, title, description)`
- 5xx response → `AppError.Network.Unexpected(message)`
- `JsonConvertException` or other unmatched non-cancellation, nonfatal causes →
  `AppError.Network.Unexpected(message)`.

Reading an error response body falls back to `null` only for `IOException`.
Cancellation and other failures propagate; do not wrap the read in broad
`runCatching(...).getOrNull()`.

---

## Domain + Unknown error origins (EXAMPLE)

A use case may decide that a domain rule failed (e.g. "cannot delete the last note pinned to an
active tag") and throw:

```kotlin
throw AppError.Expected(
    message = "Cannot delete this note",
    description = "An active tag would lose its anchor.",
)
```

Unclassified failures reaching the shared UI error pipeline are presented as
`AppErrorState.Unknown` by `ErrorProviderImpl`; this does not create a new
`AppError.Unknown` exception. `OperationManagerImpl` excludes caller cancellation.
Fatal/non-Exception Throwables bypass `safeLaunch.mapError`; logging, crash
reporting and `ErrorProvider` receive the original Throwable through the existing
UI pipeline. The network validator's rethrow does not change that UI policy.

---

## `AppError` hierarchy (EXAMPLE)

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

`AppError` lives UI-free (in `:ui-core:error:error-provider` per the template) so `:data-services:*`
and `:toolkit:http-client` can depend on it without dragging in any UI surface.

---

## Authoring rules — Repository (SHOULD)

```kotlin
// Good — runCatching converts to Result
override suspend fun saveNote(note: Note): Result<String> = runCatching {
    val body = note.toBody()
    val response = api.setNote(body).getOrThrow()
    val id = response.id ?: error("missing id")
    noteDao.insert(note.toEntity(id))
    id
}
```

```kotlin
// Explicit partial-success policy: the confirmed save does not require the optional refresh.
override suspend fun saveNote(note: Note): Result<String> {
    val response = api.setNote(note.toBody())
    return response.mapCatching { dto ->
        val id = dto.id ?: error("missing id")
        api.getNote(id).onSuccess { full ->
            full.toEntityOrNull()?.let { noteDao.insert(it) }
        }
        id
    }
}
```

The Repository returns `Result` / lets exceptions propagate via `runCatching` — never
catch-and-swallow, never return a DTO. ViewModels unwrap with `getOrThrow()` inside
`safeLaunch`; the shared pipeline presents errors. See the
[UI error rules](../../ui-feature/references/error-pipeline.md).

---

## Authoring rules — UseCase (SHOULD)

A composing `UseCase` in the **command shape** chains Features through `getOrThrow()`.
The first Result failure short-circuits by throwing the original exception. A
throwing Unit/T command does not return `Result.Failure` unless its declared
contract explicitly wraps the operation in Result:

```kotlin
public class LoginUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature,
    private val noteFeature: NoteFeature,
    private val tagFeature: TagFeature,
) {
    public suspend fun loginEmail(email: String, password: String): Boolean {
        authorizationFeature.login(email, password).getOrThrow()
        val hasProfile = userFeature.getUser().getOrThrow()
        if (hasProfile) {
            noteFeature.getNotes().getOrThrow()
            tagFeature.getTags().getOrThrow()
        }
        return hasProfile
    }
}
```

`getOrThrow()` unwraps Result; it does not validate its payload. Inspect the
callee's business contract: `false` from login/session `hasProfile` is a valid
no-profile branch. A command requiring successful profile creation and a loaded
profile must reject false outcomes before dependent hydration, welcome status or
navigation. Do not reject every Boolean false or report every Result.success as
business success.

Required cleanup must propagate its Result failure, just like required requests.
Do not silently ignore `localSettingsFeature.clear()` or other completion
prerequisites. A deliberately best-effort remote push-token deletion may discard
its returned failure under the existing documented business policy; this does not
permit swallowing caller cancellation or changing other required steps.

Keep complete operations business-owned. Alternate entry paths reaching the same
ready state must perform the required data hydration, including the existing-account
registration path and ordinary login. Verify required dependencies, false/failure
short-circuiting and successful completion through actual complete-UseCase source review and permitted manual checks
within the permitted scope; check matching UI outcomes when relevant. Do not add
analytics dependencies, split preparation for timing or introduce verification infrastructure.

Two patterns coexist in this codebase:

- **No outer `runCatching`** (as above): the call site (a ViewModel inside `safeLaunch`) wraps the
  call. Any `getOrThrow()` failure becomes a thrown exception and flows through the standard error
  pipeline.
- **Outer `runCatching`** when the use case is itself a Feature method that must return `Result<T>`
  to its caller: `runCatching { fooFeature.x().getOrThrow(); barFeature.y().getOrThrow() }`. The
  first `getOrThrow()` failure is unwrapped, then re-wrapped by the outer `runCatching` — no
  double-handling.

The **stream shape** has no `getOrThrow()` short-circuit: it `combine(...)`s the features' flows
into a combined flow and surfaces errors through that flow, not as a `Result.Failure`.
