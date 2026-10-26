# Backend transport

Source of truth: `data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/client/`
(`BackendClient.kt`, `ClientLogger.kt`, `TokenProvider.kt`), `GrippoApi.kt` beside it and
`toolkit/http-client/`. Transport changes need the authorized scope to cover network core
([scope discipline](../../../../CLAUDE.md#scope-discipline)). Inspect the files before relying
on a detail below.

## Current shape (verified)

- `toolkit/http-client` `HttpModule` (`includes = [ContextModule, SerializationModule]`) provides
  the base `HttpClient`: `NativeContext.driver()` is `HttpClient(OkHttp)` on Android
  (`PlatformDriver.android.kt`) and `HttpClient(Darwin)` with `setAllowsCellularAccess(true)` on
  iOS (`PlatformDriver.ios.kt`), configured with the shared `responseValidator(apiErrorParser)`.
- `BackendClient` (`@Single internal`) derives its client with `httpClient.config { }`:
  `HttpTimeout` with request, connect and socket timeouts of 10 s; `Logging` at `LogLevel.INFO`
  through `ClientLogger`; `Auth` with `TokenProvider`; `ContentNegotiation` with the injected
  shared `Json`; `defaultRequest` with host `grippo-app.com`, `URLProtocol.HTTPS`, JSON content
  type and `Accept-Language = AppLocale.current()` unless the request supplies the
  explicit locale override. Inspect `invoke` for method/path/body/query and locale
  forwarding; it returns `HttpResponse` inside `withContext(Dispatchers.IO)`.
- `ClientLogger` forwards every Ktor log line to `AppLogger.Network`, auth requests included; there
  is no auth-specific log sanitation or redaction marker. Do not widen logging or add bodies
  ([secrets and configuration](../../platform-build-toolkit/references/secrets-and-config.md)).
- `GrippoApi` is `@Single public class GrippoApi internal constructor(client: BackendClient)` with
  public throwing suspend endpoints and one private `request` helper; its sections,
  helper semantics and quirks are described in [DTO/API](dtos-and-api.md).
- `BackendModule` is `@Module(includes = [HttpModule, DatabaseModule, SerializationModule]) @ComponentScan`
  (`TokenProvider` reads the token DAOs).

## Shared validator (`toolkit/http-client/.../internal/ResponseValidator.kt`)

- `validateResponse`: 2xx passes; 4xx throws `AppError.Network.Expected(keys, title, description)`
  parsed from the body by `ApiErrorParser`; 5xx throws `AppError.Network.Unexpected` with the
  default server message; any other status throws `AppError.Network.Unexpected("Unexpected HTTP code: N")`.
- `handleResponseExceptionWithRequest`: rethrows non-`Exception` throwables and cancellation;
  passes an `AppError` through; maps `HttpRequestTimeoutException` to `AppError.Network.Timeout`,
  `JsonConvertException` to `AppError.Network.Unexpected("Invalid server response format.")`
  (after finding cancellation/fatal errors anywhere in the decoding cause chain, with cycle
  protection), `IOException` to `AppError.Network.NoInternet`
  and anything else to `AppError.Network.Unexpected`.
- Reading an error response body may use status-based text after an `IOException`, retaining that
  exception as the status error's cause. The error-body parser keeps its malformed-text/key
  fallbacks, but rethrows cancellation/fatal errors, including wrapped causes. Required successful
  response decoding never uses those presentation fallbacks. Inspect `ThrowableControlFailure.kt`
  for the shared control-failure check; do not generalize it into a recovery/execution wrapper.
- The validator runs in Ktor's send hook and is registered before the Auth plugin, so a validated
  failure surfaces from the request call itself, after any refresh/retry the Auth plugin performed;
  `GrippoApi.request` propagates it unchanged. `AppError.Network.IncompleteResponse` is
  raised by repositories, not by transport ([operation outcomes](operation-outcomes.md)).

## Rules

- Keep the engines, validator, serializer, locale header, timeouts and request shape. Do not invent
  hosts or a second authenticated client for a feature.
- Business layers receive transport exceptions unchanged under the throwing suspend
  contract; translation to UI belongs to `ErrorProvider`
  ([UI error pipeline](../../ui-feature/references/error-pipeline.md)).
- Refresh synchronization, retry and circuit-breaker behavior is owned by [auth session](auth-session.md).
