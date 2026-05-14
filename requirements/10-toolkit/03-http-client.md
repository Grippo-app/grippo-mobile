# `:toolkit:http-client` — Base Ktor `HttpClient`

The base `HttpClient` consumed by `BackendClient` (in `:data-services:backend`). Configures the platform engine and the `HttpResponseValidator` that converts HTTP failures into `AppError` subtypes.

## Module

```
:toolkit:http-client/
  build.gradle.kts
  src/
    commonMain/kotlin/com/<org>/<product>/toolkit/http/client/
      HttpModule.kt
      PlatformDriver.kt                  // expect fun NativeContext.driver(): HttpClient
      internal/
        ApiErrorParser.kt
        ResponseValidator.kt             // HttpClientConfig<*>.responseValidator extension
    androidMain/kotlin/com/<org>/<product>/toolkit/http/client/
      PlatformDriver.android.kt          // HttpClient(Android)
    iosMain/kotlin/com/<org>/<product>/toolkit/http/client/
      PlatformDriver.ios.kt              // HttpClient(Darwin) { engine { configureRequest { ... } } }
```

## `HttpModule`

```kotlin
@Module(includes = [ContextModule::class, SerializationModule::class])
@ComponentScan
public class HttpModule {

    @Single
    internal fun HttpClient(
        context: NativeContext,
        apiErrorParser: ApiErrorParser,
    ): HttpClient = context.driver().config {
        responseValidator(apiErrorParser)
    }
}

// commonMain — PlatformDriver.kt
internal expect fun NativeContext.driver(): HttpClient

// androidMain
internal actual fun NativeContext.driver(): HttpClient = HttpClient(Android)

// iosMain
internal actual fun NativeContext.driver(): HttpClient =
    HttpClient(Darwin) { engine { configureRequest { setAllowsCellularAccess(true) } } }
```

- The `@Module(includes = ...)` pulls in `ContextModule` (so `NativeContext` is available to the driver expect/actual) and `SerializationModule` (so `ApiErrorParser` can inject `Json`).
- The provider's function name is `HttpClient(...)` — the type's name reused as a factory. Koin still resolves it as `single<HttpClient>`.
- `responseValidator(apiErrorParser)` is a `HttpClientConfig<*>` extension defined in `internal/ResponseValidator.kt`; it installs the validator + transport-error handler.
- iOS `Darwin` engine sets `setAllowsCellularAccess(true)` so requests work on cellular without an `NSURLRequest`-level opt-in.

## `ResponseValidator`

```kotlin
internal fun HttpClientConfig<*>.responseValidator(
    apiErrorParser: ApiErrorParser,
) = HttpResponseValidator {
    validateResponse { response ->
        val statusCode = response.status.value
        if (statusCode in 200..299) return@validateResponse

        val rawBody = runCatching { response.bodyAsText() }.getOrNull()
        when (statusCode) {
            in 400..499 -> throw AppError.Network.Expected(
                keys = apiErrorParser.parseKeys(rawBody),
                title = apiErrorParser.parseDetailedMessage(rawBody, statusCode).title,
                description = apiErrorParser.parseDetailedMessage(rawBody, statusCode).description,
            )
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
            is HttpRequestTimeoutException -> throw AppError.Network.Timeout(
                message = "Request timed out. Try again.", cause = cause,
            )
            is JsonConvertException -> throw AppError.Network.Unexpected(
                message = "Invalid server response format.", cause = cause,
            )
            is IOException -> throw AppError.Network.NoInternet(
                message = "Connection lost or unavailable.", cause = cause,
            )
            else -> AppError.Network.Unexpected(
                message = cause.message ?: "Unexpected network error", cause = cause,
            )
        }
    }
}
```

- 4xx → `AppError.Network.Expected` (server-shaped error with `keys`/`title`/`description`).
- 5xx → `AppError.Network.Unexpected` (no body parsing).
- `TimeoutCancellationException` / `HttpRequestTimeoutException` → `AppError.Network.Timeout`.
- `IOException` → `AppError.Network.NoInternet`.
- `JsonConvertException` → `AppError.Network.Unexpected` (deserialization failure, e.g. schema drift).

## `ApiErrorParser`

```kotlin
@Single
internal class ApiErrorParser(private val json: Json) {

    data class ParsedError(val title: String, val description: String?)

    fun parseDetailedMessage(rawBody: String?, status: Int?): ParsedError { /* extracts message/error/reason/description from JSON */ }
    fun parseKeys(rawBody: String?): List<String> { /* reads errors[].code from JSON */ }
    fun getDefaultClientErrorMessage(status: Int?): String { /* 400/401/403/404/408/429 → human-readable */ }
    fun getDefaultServerErrorMessage(status: Int?): String { /* 500/502/503/504 → human-readable */ }
}
```

- Real type is a **`@Single internal class`** taking the shared `Json`, not an `object`.
- No `ExpectedErrorBody` data class — the parser walks `JsonElement` directly so unexpected payload shapes degrade gracefully.
- The downstream `ErrorProviderImpl` translates `AppError.Network.*` into `AppErrorState.Network.*` which becomes a `DialogConfig.ErrorDisplay`.

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.http.client" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.context)
            implementation(projects.toolkit.logger)
            implementation(projects.toolkit.serialization)
            implementation(projects.uiCore.error.errorProvider)   // for AppError

            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
```

`ContentNegotiation` / `ktor-serialization-kotlinx-json` are **not** added here — they live in `:data-services:backend`'s `BackendClient`, where the JSON `install` happens. The base client stays minimal so wrappers can layer plugins on top via `.config { ... }`.

## Rules

- **`HttpResponseValidator` is the only place that throws `AppError`.** Repository / Feature / UseCase rely on this — they convert via `runCatching { ... }` not via manual status checks.
- **Engine selection is `expect/actual`** through `NativeContext.driver()`. Android uses `HttpClient(Android)`; iOS uses `HttpClient(Darwin) { engine { configureRequest { setAllowsCellularAccess(true) } } }`. No other engines.
- **`BackendClient` extends this via `.config { ... }`**, adding Auth, Logging, ContentNegotiation, timeouts, default request. The base client is intentionally minimal so wrappers can extend it.

## Anti-patterns

- **Throwing raw `Throwable` from `validateResponse`.** Always an `AppError` subtype.
- **Multiple `HttpClient` instances.** One base; extend via `.config { ... }`.
- **Different engines per environment.** The platform engine is the only choice. Mocking the engine in tests is fine via Ktor's `MockEngine`, but not in production.
- **Treating `ApiErrorParser` as an object/singleton without DI.** It's a `@Single` class that depends on the shared `Json`; reach it via Koin.
