# `BackendClient` — Ktor HTTP wrapper

Self-contained reference for the Ktor client rules.

> **Illustrative domain.** Code uses `Note` / `Tag` / `User` as the generic `<Entity>` /
> `<RelatedEntity>`. Substitute identifiers from your product domain.

`BackendClient` is the **single** Ktor `HttpClient` configured for the product backend. Lives
in `:data-services:backend`. `<Product>Api` is its only public consumer.

---

## Class shape (EXAMPLE)

```kotlin
@Single
internal class BackendClient(
    httpClient: HttpClient,
    tokenProvider: TokenProvider,
    clientLogger: ClientLogger,
    json: Json,
) {
    private val clientProvider: HttpClient = httpClient.config {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }

        install(Logging) {
            level = LogLevel.ALL
            logger = clientLogger
        }

        install(Auth) {
            providers.add(tokenProvider)
        }

        install(ContentNegotiation) {
            json(
                json = json,
                contentType = ContentType.Application.Json,
            )
        }

        defaultRequest {
            host = "<product-domain>"
            url { protocol = URLProtocol.HTTPS }
            contentType(ContentType.Application.Json)
            header(HttpHeaders.AcceptLanguage, AppLocale.current())
        }
    }

    suspend fun invoke(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
    ): HttpResponse = withContext(Dispatchers.IO) {
        clientProvider.request {
            url {
                path(path)
                queryParams?.forEach { (k, v) -> parameters.append(k, v) }
            }
            this.method = method
            body?.let { setBody(body) }
        }
    }
}
```

---

## Construction (REFERENCE)

The base `httpClient: HttpClient` comes from `:toolkit:http-client`'s `HttpModule`:

```kotlin
// :toolkit:http-client
@Module(includes = [ContextModule::class, SerializationModule::class])
@ComponentScan
public class HttpModule {

    @Single
    internal fun HttpClient(
        context: NativeContext,
        apiErrorParser: ApiErrorParser,
    ): HttpClient {
        return context.driver().config {
            responseValidator(apiErrorParser)
        }
    }
}
```

- `context.driver()` is an `internal expect fun NativeContext.driver(): HttpClient` with
  platform actuals: `HttpClient(Android)` on `androidMain`,
  `HttpClient(Darwin) { engine { configureRequest { setAllowsCellularAccess(true) } } }` on `iosMain`.
- `responseValidator(apiErrorParser)` is an internal `HttpClientConfig<*>` extension in
  `:toolkit:http-client/internal/ResponseValidator.kt` that wires Ktor's
  `HttpResponseValidator { validateResponse { ... } handleResponseExceptionWithRequest { ... } }`.
  It maps non-2xx statuses to `AppError.Network.Expected` / `Unexpected` and translates
  `HttpRequestTimeoutException` / `JsonConvertException` / `IOException` into
  `AppError.Network.Timeout` / `Unexpected` / `NoInternet`. `ApiErrorParser` (`@Single`)
  extracts a title/description and `errors[].code` keys from the JSON body.

`BackendClient` `.config { ... }` derives a configured copy of the base client. **Don't** call
`HttpClient(...)` again — reuse the base via `.config`.

---

## Installed plugins

### `HttpTimeout` (NORMATIVE)

All three timeouts at 10 seconds:

- `requestTimeoutMillis = 10_000` — total request duration (including retries).
- `connectTimeoutMillis = 10_000` — TCP/TLS handshake.
- `socketTimeoutMillis = 10_000` — between bytes once connected.

These match for simplicity. For long-running endpoints (uploads, downloads), pass a per-call
override at the call site.

### `Logging` (NORMATIVE)

```kotlin
install(Logging) {
    level = LogLevel.ALL
    logger = clientLogger
}
```

`LogLevel.ALL` includes URL, method, headers, body. `clientLogger` (`ClientLogger`) routes the
log line to `AppLogger.Network.log(...)` with color emojis for fast visual scanning (🟩 success,
🟥 error, 🟨 request). In release builds, scale down to `LogLevel.HEADERS` to avoid logging
request/response bodies — body logging in release is a PII risk. Log truncation handling is the
responsibility of the file writer in `:toolkit:logger`.

### `Auth` (NORMATIVE)

```kotlin
install(Auth) {
    providers.add(tokenProvider)
}
```

`tokenProvider: TokenProvider` injects bearer tokens, handles 401 refresh with mutex + retry +
circuit breaker. See [`auth-session.md`](auth-session.md).

### `ContentNegotiation` (NORMATIVE)

```kotlin
install(ContentNegotiation) {
    json(json = json, contentType = ContentType.Application.Json)
}
```

`json: Json` from `:toolkit:serialization`'s `SerializationModule`:

```kotlin
@Module
@ComponentScan
public class SerializationModule {
    @Single
    internal fun provideJson(): Json = Json {
        useAlternativeNames = false
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
}
```

- `ignoreUnknownKeys = true` — backend adding new fields without coordination doesn't break the client.
- `isLenient = true` — accepts minor JSON deviations (trailing commas, unquoted keys) defensively.
- `useAlternativeNames = false` — disables `@JsonNames` matching; only the canonical `@SerialName` is accepted.
- `prettyPrint = true` — multi-line output makes the Ktor `Logging` plugin's request/response dump readable in the log file.

---

## `defaultRequest` (MUST)

```kotlin
defaultRequest {
    host = "<product-domain>"
    url { protocol = URLProtocol.HTTPS }
    contentType(ContentType.Application.Json)
    header(HttpHeaders.AcceptLanguage, AppLocale.current())
}
```

- **`host`** is the backend base host. Hardcoded — `:data-services:backend` has one consumer (the product).
- **`URLProtocol.HTTPS`** — non-negotiable. HTTP is not used.
- **`contentType = JSON`** — all requests are JSON unless overridden per-call.
- **`Accept-Language = AppLocale.current()`** — set on **every** request. Backend uses this to
  localize response strings.

For multi-env setups (staging / production), parameterize the host (see "Multi-environment").

---

## `invoke(method, path, body, queryParams)` (NORMATIVE)

```kotlin
suspend fun invoke(
    method: HttpMethod,
    path: String,
    body: Any? = null,
    queryParams: Map<String, String>? = null,
): HttpResponse = withContext(Dispatchers.IO) {
    clientProvider.request {
        url {
            path(path)
            queryParams?.forEach { (k, v) -> parameters.append(k, v) }
        }
        this.method = method
        body?.let { setBody(body) }
    }
}
```

- **Returns raw `HttpResponse`.** Callers do `.body()` to deserialize.
- **Wrapped in `Dispatchers.IO`** so callers can call `invoke` on `Main` safely.
- **Single method.** Every HTTP verb goes through here. `<Product>Api` constructs
  `HttpMethod.Get`, `Post`, `Put`, `Delete` as needed.

---

## How `<Product>Api` uses it (EXAMPLE)

> Full `<Product>Api` in [`dtos-and-api.md`](dtos-and-api.md).

```kotlin
@Single
public class <Product>Api internal constructor(private val client: BackendClient) {

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
    ): Result<T> = runCatching {
        client.invoke(method, path, body, queryParams).body<T>()
    }

    public suspend fun get<Entities>(start: String, end: String): Result<List<<Entity>Response>> =
        request(
            method = HttpMethod.Get,
            path = "/<entity_table>s",
            queryParams = mapOf("start" to start, "end" to end),
        )

    // ... one method per endpoint
}
```

The `request<T>` private inline helper wraps `client.invoke(...)` + `.body()` in `runCatching`.
Result of every endpoint is `Result<T>`.

---

## Error path (REFERENCE)

> Full error path in [`error-pipeline.md`](error-pipeline.md).

`HttpResponseValidator` in `:toolkit:http-client` throws `AppError` subtypes for non-success
responses. Inside `runCatching`, these become `Result.Failure(AppError.*)`. The ViewModel calls
`.getOrThrow()` to surface them through `safeLaunch` → `ErrorProvider`.

---

## Multi-environment (SHOULD)

To swap base hosts (staging/production):

```kotlin
// option A: Koin parameter
@Module
public class BackendModule {
    @Single
    internal fun provideBackendHost(): BackendHost = BackendHost(BuildConfig.BACKEND_HOST)
}

internal data class BackendHost(val value: String)

// in BackendClient:
defaultRequest {
    host = backendHost.value
    // ...
}
```

Where `BuildConfig.BACKEND_HOST` comes from `:androidApp/build.gradle.kts` and an `iosApp`
Info.plist value. For a single-environment product, the hardcoded host is fine.

---

## Anti-patterns (MUST)

- **Creating a second `HttpClient`** for a one-off call. Reuse `BackendClient.invoke`.
- **Calling `.body<T>()` outside the `request<T>` helper** — bypasses the `Result` wrapping.
- **Switching to `Dispatchers.Default`** for an HTTP call. IO is IO.
- **Skipping `AppLocale.current()` in the `Accept-Language` header** — backend can't localize responses.
- **Disabling `Auth` for a specific call** — login, refresh, and public requests set
  `attributes.put(AuthCircuitBreaker, Unit)` at the call site; do not add ad-hoc
  `Auth` bypass paths.
- **Logging request bodies in release.** PII risk. Scale logging level for production.
