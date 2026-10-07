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

        val rawBody = runCatching { response.bodyAsText() }.getOrNull()
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
        when (cause) {
            is AppError.Network.Expected -> throw cause
            is TimeoutCancellationException,
            is HttpRequestTimeoutException -> throw AppError.Network.Timeout(message = "Request timed out. Try again.", cause = cause)
            is JsonConvertException -> throw AppError.Network.Unexpected(message = "Invalid server response format.", cause = cause)
            is IOException -> throw AppError.Network.NoInternet(message = "Connection lost or unavailable.", cause = cause)
            else -> throw AppError.Network.Unexpected(message = cause.message ?: "Unexpected network error", cause = cause)
        }
    }
}
```

The mapping the validator produces:

- `IOException` (transport-level no internet) → `AppError.Network.NoInternet`
- `TimeoutCancellationException` / `HttpRequestTimeoutException` → `AppError.Network.Timeout`
- 4xx response with a parseable body → `AppError.Network.Expected(keys, title, description)`
- 5xx response → `AppError.Network.Unexpected(message)`
- `JsonConvertException` or any other unmatched cause → `AppError.Network.Unexpected(message)`

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

Any other `Throwable` falls through as `AppError.Unknown` after `ErrorProviderImpl` defaults the
mapping.

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
// Also good — explicit Result map/onSuccess chain (when partial success matters)
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
catch-and-swallow, never return a DTO. Error translation is the ViewModel's job.

---

## Authoring rules — UseCase (SHOULD)

A composing `UseCase` in the **command shape** chains multiple Features through `getOrThrow()` so
the first failure short-circuits and propagates as a `Result.Failure`:

```kotlin
public class LoginUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature,
    private val noteFeature: NoteFeature,
    private val tagFeature: TagFeature,
) {
    public suspend fun executeEmail(email: String, password: String): Boolean {
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
