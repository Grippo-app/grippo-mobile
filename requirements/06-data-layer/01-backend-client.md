# `BackendClient` — Ktor HTTP wrapper

`BackendClient` is the **single** Ktor `HttpClient` configured for the product backend. Lives in `:data-services:backend`. `GrippoApi` is its only public consumer.

## Class shape

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
            host = "<your-product-domain>.com"
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
                body?.let { setBody(body) }
            }
            this.method = method
        }
    }
}
```

## Construction

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

- `context.driver()` is an `internal expect fun NativeContext.driver(): HttpClient` defined in `:toolkit:http-client` with platform actuals: `HttpClient(Android)` on `androidMain`, `HttpClient(Darwin) { engine { configureRequest { setAllowsCellularAccess(true) } } }` on `iosMain`.
- `responseValidator(apiErrorParser)` is an internal `HttpClientConfig<*>` extension in `:toolkit:http-client/internal/ResponseValidator.kt` that wires Ktor's `HttpResponseValidator { validateResponse { ... } handleResponseExceptionWithRequest { ... } }`. It maps non-2xx statuses to `AppError.Network.Expected` / `Unexpected` and translates `HttpRequestTimeoutException` / `JsonConvertException` / `IOException` into `AppError.Network.Timeout` / `Unexpected` / `NoInternet`. `ApiErrorParser` (`@Single`) extracts a title/description and `errors[].code` keys from the JSON body.

`BackendClient` `.config { ... }` derives a configured copy of the base client. **Don't** call `HttpClient(...)` again — reuse the base via `.config`.

## Installed plugins

### `HttpTimeout`

All three timeouts at 10 seconds:

- `requestTimeoutMillis = 10_000` — total request duration (including retries).
- `connectTimeoutMillis = 10_000` — TCP/TLS handshake.
- `socketTimeoutMillis = 10_000` — between bytes once connected.

These match for simplicity. For long-running endpoints (uploads, downloads), pass a per-call override at the call site.

### `Logging`

```kotlin
install(Logging) {
    level = LogLevel.ALL
    logger = clientLogger
}
```

`LogLevel.ALL` includes URL, method, headers, body. `clientLogger` (`ClientLogger`) routes the log line to `AppLogger.Network.log(...)` with color emojis for fast visual scanning (🟩 success, 🟥 error, 🟨 request). See `06-data-layer/03-grippo-api-and-dtos.md` for `ClientLogger`.

In release builds, consider scaling down to `LogLevel.HEADERS` to avoid logging request/response bodies. Log truncation handling is the responsibility of the file writer in `:toolkit:logger`.

### `Auth`

```kotlin
install(Auth) {
    providers.add(tokenProvider)
}
```

`tokenProvider: TokenProvider` injects bearer tokens, handles 401 refresh with mutex + retry + circuit breaker. See `06-data-layer/02-token-provider.md`.

### `ContentNegotiation`

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

## `defaultRequest`

```kotlin
defaultRequest {
    host = "<your-product-domain>.com"
    url { protocol = URLProtocol.HTTPS }
    contentType(ContentType.Application.Json)
    header(HttpHeaders.AcceptLanguage, AppLocale.current())
}
```

- **`host`** is the backend base host. Hardcoded — `:data-services:backend` has one consumer (the product).
- **`URLProtocol.HTTPS`** — non-negotiable. HTTP is not used.
- **`contentType = JSON`** — all requests are JSON unless overridden per-call.
- **`Accept-Language = AppLocale.current()`** — set on **every** request. Backend uses this to localize response strings.

For multi-env setups (staging / production), parameterize the host via a build config or a Koin parameter — see `06-data-layer/01-backend-client.md`'s "Environments" section in your concrete project.

## `invoke(method, path, body, queryParams)`

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
            body?.let { setBody(body) }
        }
        this.method = method
    }
}
```

- **Returns raw `HttpResponse`.** Callers do `.body()` to deserialize.
- **Wrapped in `Dispatchers.IO`** so callers can call `invoke` on `Main` safely.
- **Single method.** Every HTTP verb goes through here. `GrippoApi` constructs `HttpMethod.Get`, `Post`, `Put`, `Delete` as needed.

## How `GrippoApi` uses it

```kotlin
@Single
public class GrippoApi internal constructor(private val client: BackendClient) {

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
    ): Result<T> = runCatching {
        client.invoke(method, path, body, queryParams).body()
    }

    public suspend fun getTrainings(start: String, end: String): Result<List<TrainingResponse>> =
        request(
            method = HttpMethod.Get,
            path = "/trainings",
            queryParams = mapOf("start" to start, "end" to end),
        )

    // ... one method per endpoint
}
```

The `request<T>` private inline helper wraps `client.invoke(...)` + `.body()` in `runCatching`. Result of every endpoint is `Result<T>`.

## Error path

`HttpResponseValidator` in `:toolkit:http-client` throws `AppError` subtypes for non-success responses. Inside `runCatching`, these become `Result.Failure(AppError.*)`. The ViewModel calls `.getOrThrow()` to surface them through `safeLaunch` → `ErrorProvider`.

See `03-architecture-patterns/07-error-pipeline.md`.

## Multi-environment

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

Where `BuildConfig.BACKEND_HOST` comes from `:androidApp/build.gradle.kts` and an `iosApp` Info.plist value. For a single-environment product, the hardcoded host is fine.

## Anti-patterns

- **Creating a second `HttpClient`** for a one-off call. Reuse `BackendClient.invoke`.
- **Calling `.body<T>()` outside the `request<T>` helper** — bypasses the `Result` wrapping.
- **Switching to `Dispatchers.Default`** for an HTTP call. IO is IO.
- **Skipping `AppLocale.current()` in the `Accept-Language` header** — backend can't localize responses.
- **Disabling `Auth` for a specific call** — the right way is to add a path to `TokenProvider.sendWithoutRequest` (handled internally) or use `attributes.put(AuthCircuitBreaker, Unit)` only for the explicit refresh.
- **Logging request bodies in release.** PII risk. Scale logging level for production.
