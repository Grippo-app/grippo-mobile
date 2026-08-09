# Toolkit utilities (core) — context, http-client, serialization, logger, date-utils

These are the per-`:toolkit:*` module specs for the five core utility modules: `:toolkit:context`, `:toolkit:http-client`, `:toolkit:serialization`, `:toolkit:logger`, and `:toolkit:date-utils`. Each section preserves the module's API signatures, code, rules, and anti-patterns verbatim.

## :toolkit:context

### Shape

```kotlin
// commonMain
public expect class NativeContext

// androidMain
public actual class NativeContext(public val context: Context)

// iosMain
public actual class NativeContext
```

### Provider

```kotlin
// commonMain
@Module
@ComponentScan
public expect class ContextModule() {
    @Single
    internal fun providesNativeContext(scope: Scope): NativeContext
}

// androidMain
@Module
@ComponentScan
public actual class ContextModule actual constructor() {
    @Single
    internal actual fun providesNativeContext(scope: Scope): NativeContext =
        NativeContext(scope.get<Application>())
}

// iosMain
@Module
@ComponentScan
public actual class ContextModule actual constructor() {
    @Single
    internal actual fun providesNativeContext(scope: Scope): NativeContext =
        NativeContext()
}
```

The Android `Application` comes from `androidContext(this@App)` set in `Koin.init { ... }` — Koin's `androidContext()` plugin registers the supplied instance under its concrete `Application` type. The provider reads it via `scope.get<Application>()`. `@Module` and `@ComponentScan` must be repeated on each `actual` class — KSP runs against each platform compilation and the annotations don't propagate from `expect` to `actual`.

### Usage

```kotlin
// :data-services:database/DatabaseModule.kt
@Module(includes = [ContextModule::class])
@ComponentScan
public class DatabaseModule {
    @Single
    internal fun provideDatabase(nativeContext: NativeContext): Database =
        nativeContext.getDatabaseBuilder()
}

// :data-services:database/DatabaseBuilder.kt
// commonMain
internal expect fun NativeContext.getDatabaseBuilder(): Database

// androidMain
internal actual fun NativeContext.getDatabaseBuilder(): Database {
    val appContext = this.context.applicationContext
    val dbFile = appContext.getDatabasePath("...")
    return Room.databaseBuilder<Database>(context = appContext, name = dbFile.absolutePath)
        // ...
        .build()
}

// iosMain
internal actual fun NativeContext.getDatabaseBuilder(): Database {
    val dbPath = documentDirectory() + "/<product>_database.db"
    return Room.databaseBuilder<Database>(name = dbPath)
        // ...
        .build()
}
```

The pattern: `commonMain` declares `NativeContext.<verb>()` as `expect`; per-platform `actual` extension functions consume the platform handle.

### Why this pattern

- **Android** needs a `Context` for everything platform-touching (filesystem, system services, intents). `commonMain` cannot import `Context`.
- **iOS** doesn't have an equivalent global handle; most APIs are stateless objects (`NSFileManager.defaultManager`, `UIApplication.sharedApplication`).
- **`NativeContext`** encapsulates the asymmetry: Android implementations cast to `this.context`; iOS implementations don't need the parameter at all, but accepting `NativeContext` keeps the signature platform-neutral.

### Rules

- **`NativeContext` is the only platform-handle escape**. No other `expect/actual` class wraps `Context`.
- **The Android `Application` registered in Koin must be the `Application` instance**, not an Activity. `androidContext(this@App)` in `App.onCreate` does this — Koin registers it under its `Application` type.
- **`scope.get<Application>()` is fine inside `ContextModule`**, but DON'T pull `Application`/`Context` directly elsewhere. Use `NativeContext`.
- **iOS code doesn't import `NativeContext` for anything**. The iOS `actual` is an empty class; extension functions in `iosMain` just need to take `NativeContext` as the receiver to match the `expect` signature.

### Anti-patterns

- **`Context` import in `commonMain`.** Compile error (correctly).
- **Bypassing `NativeContext`** by exposing a `Context` provider in Koin. The Application-vs-Activity confusion bites.
- **Storing a non-Application `Context`** in `NativeContext`. Hold the Application context; pass an Activity context locally when needed.
- **iOS `NativeContext` with actual state.** Keep it empty unless there's a specific reason (e.g. holding a `UIApplication` reference).

## :toolkit:http-client

### Module

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

### `HttpModule`

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

### `ResponseValidator`

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
            else -> throw AppError.Network.Unexpected(
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

### `ApiErrorParser`

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

### Build

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

### Rules

- **`HttpResponseValidator` is the only place that throws `AppError`.** Repository / Feature / UseCase rely on this — they convert via `runCatching { ... }` not via manual status checks.
- **Engine selection is `expect/actual`** through `NativeContext.driver()`. Android uses `HttpClient(Android)`; iOS uses `HttpClient(Darwin) { engine { configureRequest { setAllowsCellularAccess(true) } } }`. No other engines.
- **`BackendClient` extends this via `.config { ... }`**, adding Auth, Logging, ContentNegotiation, timeouts, default request. The base client is intentionally minimal so wrappers can extend it.

### Anti-patterns

- **Throwing raw `Throwable` from `validateResponse`.** Always an `AppError` subtype.
- **Multiple `HttpClient` instances.** One base; extend via `.config { ... }`.
- **Different engines per environment.** The platform engine is the only choice. Mocking the engine in tests is fine via Ktor's `MockEngine`, but not in production.
- **Treating `ApiErrorParser` as an object/singleton without DI.** It's a `@Single` class that depends on the shared `Json`; reach it via Koin.

## :toolkit:serialization

### Module

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

### Configuration rationale

| Option | Value | Why |
|---|---|---|
| `useAlternativeNames` | `false` | Disables `@JsonNames` alternative lookups — DTOs are matched by `@SerialName` only, which keeps the wire contract one-to-one with the backend |
| `ignoreUnknownKeys` | `true` | Backend may add fields without coordinating a mobile release; old clients ignore unknowns |
| `isLenient` | `true` | Accepts trailing commas, unquoted keys — defensive against malformed responses |
| `prettyPrint` | `true` | Outgoing bodies are pretty-printed; makes the network log readable. Trivial wire-size cost — payloads are small |

These four flags together are the **defensive defaults**. Don't change without a strong reason.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.serialization" }

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}
```

### Usage

The `Json` instance is injected wherever JSON serialization/deserialization happens:

```kotlin
// :data-services:backend/BackendClient.kt
install(ContentNegotiation) {
    json(json = json, contentType = ContentType.Application.Json)
}
```

```kotlin
// elsewhere (rare — most serialization goes through Ktor)
class MyService(private val json: Json) {
    fun serialize(value: MyData): String = json.encodeToString(value)
    fun deserialize(text: String): MyData = json.decodeFromString(text)
}
```

### Rules

- **One `Json` instance app-wide.** Different configs lead to subtle bugs.
- **Don't `Json { ... }` ad-hoc.** Inject the shared instance via Koin.
- **`@Serializable` on every DTO** that goes through this `Json`. Compile-time-checked via the `kotlin-serialization` plugin.

### When to add a different config

Almost never. If you genuinely need a second `Json` (e.g. one for backend with `ignoreUnknownKeys = true`, one for an internal protocol with `ignoreUnknownKeys = false`), add a named provider:

```kotlin
@Module
@ComponentScan
public class SerializationModule {
    @Single
    internal fun provideJson(): Json = Json { ... defensive defaults ... }

    @Single
    @Named("strict")
    internal fun provideStrictJson(): Json = Json {
        ignoreUnknownKeys = false
        // ...
    }
}
```

Consumers `inject<Json>(named("strict"))`. But weigh the cost — two configs mean two compile-time validators.

### Anti-patterns

- **`Json.Default`** in production code — has `encodeDefaults = false` and other quirks.
- **`Json { isLenient = false; ignoreUnknownKeys = false }`** — strict modes break on backend evolution.
- **Multiple `Json` instances created inline** in different services. Single source of truth.
- **`@Transient` on serialized fields you actually want to send.** `@Transient` skips serialization; for non-serialized callback lambdas it's correct (see the ui-feature skill, references/dialogs.md), but for data fields it's a bug.

## :toolkit:logger

### Object signature

```kotlin
public object AppLogger {
    public fun logFileContentsByCategory(): Map<String, List<String>>
    public fun clearLogFile(): Boolean

    public object General {
        public fun error(msg: String): Unit
        public fun warning(msg: String): Unit
    }

    public object Navigation {
        public fun log(msg: String): Unit
    }

    public object Network {
        public fun log(msg: String): Unit
    }

    public object Mapping {
        public fun <T> log(value: T?, msg: () -> String): T?
    }
}
```

### Categories

| Category | Used by |
|---|---|
| `General` | ViewModels, Repositories, the error pipeline (`AppLogger.General.error(exception.stackTraceToString())`) |
| `Navigation` | Decompose-related logs (route push/pop), `RootViewModel` deeplinks |
| `Network` | `ClientLogger` routes Ktor `Logging` here |
| `Mapping` | DTO → Entity / Domain mappers' null logging |

Each category writes to the same file but tags the line with the category name. The debug screen filters by category.

### `Mapping.log` — the null-tracker

```kotlin
public fun <T> log(value: T?, msg: () -> String): T? {
    if (value != null) return value
    // file-write "[MAPPING] $msg (file:line)"
    return null
}
```

- Returns the input value verbatim — no side effect on the value flow.
- Logs only when the value is null.
- The `msg` lambda is **lazy** — only invoked when needed.
- Call sites are the four directions whose **source** carries nullable values — `:data-mappers:dto-to-entity`, `:data-mappers:dto-to-domain`, `:data-mappers:entity-to-domain` (`@Relation` rows + enum-string parses), and `:data-mappers:state-to-domain` (`*FormatState.value`) — for required-field checks. See the mappers skill, references/mapper-directions.md and references/null-safety-and-logging.md.

### File location

| Platform | Path |
|---|---|
| Android | `user.home/<product>/logs/app.log`, falling back to `java.io.tmpdir/<product>/logs/app.log` or `/tmp/<product>/logs/app.log` |
| iOS | `NSTemporaryDirectory()/<product>/logs/app.log` |

The Android implementation reads the JVM `user.home` property and only falls back to `tmpdir` if that's blank. On iOS the file lives in the temporary directory — survives app restarts but the system may purge it under storage pressure.

### File rotation

The current `LogFileWriter` appends without size-based rotation; `AppLogger.clearLogFile()` is the explicit reset path (it deletes the file and re-creates the writer).

### Public read API

```kotlin
public fun logFileContentsByCategory(): Map<String, List<String>>
```

Returns the current log file's contents grouped by category. Used by:

- The debug screen — shows recent logs.
- Bug reports — attach the log file.

```kotlin
public fun clearLogFile(): Boolean
```

Empties the log file. Returns `true` on success.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.logger" }

    sourceSets.commonMain.dependencies {
        implementation(libs.datetime)
    }
}
```

No `koin.annotation.convention` — `AppLogger` is a plain `object` accessed statically, not a `@Single`. No dependency on `:toolkit:context` either; the file path is resolved through JVM/`NSFileManager` system APIs inside the platform `actual`s.

### Usage

#### In a ViewModel

```kotlin
// not typical — errors flow through the pipeline automatically
AppLogger.General.warning("User attempted to save with invalid form")
```

The error pipeline calls `AppLogger.General.error(...)` automatically; explicit calls are rare and reserved for unusual diagnostic needs.

#### In a Mapper

```kotlin
val id = AppLogger.Mapping.log(dto.id) { "NoteResponse.id is null" } ?: return null
```

#### In `ClientLogger` (Ktor's `Logger`)

```kotlin
@Single
internal class ClientLogger : Logger {
    override fun log(message: String) {
        // ... emoji formatting
        AppLogger.Network.log("$emojiLine HTTP LOG $emojiLine\n$message")
    }
}
```

#### Navigation

`RootViewModel` may log when a deeplink fires or when a back navigation cascades:

```kotlin
AppLogger.Navigation.log("Deeplink: ${deeplink.key}")
```

Used sparingly. Default is no Navigation logs — `Navigation` category is opt-in for specific flows.

### Why a single logger object

- **Visible across all layers.** A Mapper, a ViewModel, and the network layer all use the same call.
- **Centralized file write.** No race conditions between competing writers.
- **Multiplatform.** Built on top of `expect/actual` file handles.

`println(...)` and platform-specific loggers (`android.util.Log`, `NSLog`) are forbidden in production code. Local debugging via `println` is fine while developing — but remove before committing.

### Rules

- **`AppLogger.General.error(...)` is invoked by `BaseViewModel.sendError` automatically.** Don't double-log.
- **Don't log PII.** Truncate access tokens (`bearer.take(25)`), hash user IDs, omit emails / passwords entirely.
- **Don't log inside tight loops** — use `Mapping.log` only at boundary translations.
- **Don't depend on logs for correctness** — logging is observability, not a side-channel for state.

### Anti-patterns

- **`println(...)`** in production code.
- **`System.err.println(...)`** — won't work on iOS.
- **`android.util.Log.e(...)`** in `commonMain` — won't compile.
- **`AppLogger.General.error(...)` after a Ktor exception** — the error pipeline does this. Duplicate Firebase reports.
- **Logging request/response bodies in release builds** without scrubbing.

## :toolkit:date-utils

### `DateTimeUtils`

```kotlin
public object DateTimeUtils {

    public fun now(): LocalDateTime
    public fun asInstant(value: LocalDateTime): Instant

    // Date range factories
    public fun thisDay(): DateRange
    public fun leadingYear(): DateRange
    public fun thisWeek(): DateRange
    public fun thisMonth(): DateRange
    public fun thisYear(): DateRange
    public fun trailingYear(): DateRange
    public fun trailingMonth(): DateRange
    public fun trailing14Days(): DateRange
    public fun trailing60Days(): DateRange
    public fun trailingWeek(): DateRange
    public fun infinity(): DateRange

    // Time for date
    public fun startOfDay(value: LocalDateTime): LocalDateTime
    public fun endOfDay(value: LocalDateTime): LocalDateTime
    public fun startOfDay(value: LocalDate): LocalDateTime
    public fun endOfDay(value: LocalDate): LocalDateTime

    // ISO conversion (UTC)
    public fun toUtcIso(value: LocalDateTime): String
    public fun toLocalDateTime(timestamp: String): LocalDateTime

    // Formatting
    public fun format(value: LocalDateTime, format: DateFormat): String
    public fun format(value: LocalTime, format: DateFormat.TimeOnly): String
    public fun format(value: LocalDate, format: DateFormat.DateOnly): String
    public fun format(duration: Duration): String

    // Math
    public fun ago(value: LocalDateTime): Duration
    public fun minus(value: LocalDateTime, minus: Duration): LocalDateTime
    public fun plus(value: LocalDateTime, plus: Duration): LocalDateTime
    public fun shift(range: DateRange, period: DatePeriod): DateRange

    // Timeline predicates
    public fun isToday(date: LocalDate): Boolean
    public fun isYesterday(date: LocalDate): Boolean
    public fun isTomorrow(date: LocalDate): Boolean
    public fun isFuture(date: LocalDate): Boolean
    public fun isPast(date: LocalDate): Boolean
    public fun isPast(value: LocalDateTime): Boolean

    // Static helpers
    public fun getDaysInMonth(year: Int, month: Month): Int
    public fun weekDayShortLabels(): List<String>
}
```

### `DateRange`

```kotlin
@Serializable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
) {
    public fun isWellFormed(): Boolean
    public fun coerceWithin(limitations: DateRange): DateRange
}
```

- `@Serializable` so it can live in `*Router` payloads.
- `isWellFormed()` returns `from <= to`.
- `coerceWithin(limitations)` clamps to a bound range.

### `DateRangeKind`

```kotlin
@Serializable
public enum class DateRangeKind {
    Daily,
    Weekly,
    Last7Days,
    Last14Days,
    Monthly,
    Last30Days,
    Last60Days,
    Last365Days,
    Yearly,
    Infinity,
    Custom,
}
```

`Custom` is used when the user picks arbitrary dates that don't match any preset.

### `DateRangePresets`

```kotlin
public object DateRangePresets {
    public fun daily(): DateRange
    public fun weekly(): DateRange
    public fun last7Days(): DateRange
    public fun last14Days(): DateRange
    public fun monthly(): DateRange
    public fun last30Days(): DateRange
    public fun last60Days(): DateRange
    public fun last365Days(): DateRange
    public fun yearly(): DateRange
    public fun infinity(): DateRange

    public fun resolve(kind: DateRangeKind): DateRange?
    public fun classify(range: DateRange): DateRangeKind
}
```

- `resolve(kind)` returns the concrete range for a preset, or `null` for `Custom`.
- `classify(range)` reverses: if the range exactly matches a preset's bounds, returns the preset kind; otherwise `Custom`.

### `DateFormat`

```kotlin
@Immutable
@Serializable
public sealed interface DateFormat {
    public val pattern: String

    @Serializable
    public sealed interface DateOnly : DateFormat {
        public data object MonthFull : DateOnly
        public data object MonthFullStandalone : DateOnly
        public data object MonthShort : DateOnly
        public data object MonthShortStandalone : DateOnly
        public data object DateMmmDdYyyy : DateOnly        // "Jan 5, 2026"
        public data object MmmYyyy : DateOnly              // "Jan 2026"
        public data object DateMmmDdComma : DateOnly       // "Jan 5,"
        public data object DateDdMmm : DateOnly            // "5 Jan"
        public data object Mmmm : DateOnly                 // "January"
        public data object DateDdMmmm : DateOnly           // "5 January"
        public data object WeekdayShort : DateOnly         // "Mon"
        public data object WeekdayLong : DateOnly          // "Monday"
    }

    @Serializable
    public sealed interface TimeOnly : DateFormat {
        public data object Time24hHm : TimeOnly             // "14:30"
    }
}
```

### `DateFormatting`

```kotlin
public object DateFormatting {
    internal var current: DateFormatter
        private set

    public fun install(tag: String?)
}
```

`DateFormatting.install(localeTag)` swaps the internal `DateFormatter` to one configured for the given locale. Called from `RootComponent.Render()`:

```kotlin
LaunchedEffect(systemLocaleTag) {
    DateFormatting.install(systemLocaleTag)
}
```

After install, every `DateTimeUtils.format(...)` call uses the new locale. Without `install`, formatters use the system locale at process start — which may diverge if the user changes the language while the app is running.

### Usage

#### Ranges in state

```kotlin
@Immutable
@Serializable
internal data class NotesListState(
    val range: DateRange = DateRangePresets.last30Days(),
    val rangeKind: DateRangeKind = DateRangeKind.Last30Days,
    // ...
)
```

#### Range arithmetic

```kotlin
// "Previous month"
val prev = DateTimeUtils.shift(state.range, period = DatePeriod(months = -1))
```

#### Formatting

```kotlin
val label = DateTimeUtils.format(
    note.createdAt,
    DateFormat.DateOnly.DateMmmDdYyyy,
)  // "Jan 5, 2026"

val durationLabel = DateTimeUtils.format(duration)  // "1h 23m"
```

#### ISO-UTC for backend

```kotlin
val startUtc = DateTimeUtils.toUtcIso(range.from)        // "2026-01-05T00:00:00Z"
val endUtc = DateTimeUtils.toUtcIso(range.to)
api.getNotes(startUtc, endUtc)
```

#### Parsing from backend

```kotlin
val createdAt = DateTimeUtils.toLocalDateTime(dto.createdAt ?: return null)
```

`toLocalDateTime` assumes input is ISO-8601 UTC. The result is in the **system zone** — so 14:00 UTC in the backend response renders as 14:00 in the user's local time only if they're in UTC; otherwise it's converted to their wall time.

#### Timeline labels

```kotlin
@Composable
fun friendlyDate(date: LocalDate): String = when {
    DateTimeUtils.isToday(date) -> AppTokens.strings.res(Res.string.today)
    DateTimeUtils.isYesterday(date) -> AppTokens.strings.res(Res.string.yesterday)
    DateTimeUtils.isTomorrow(date) -> AppTokens.strings.res(Res.string.tomorrow)
    else -> DateTimeUtils.format(date, DateFormat.DateOnly.DateMmmDdYyyy)
}
```

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.date.utils" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)   // for Res.string localized labels
        implementation(projects.designSystem.core)
        implementation(projects.toolkit.logger)
        implementation(compose.foundation)
        implementation(libs.datetime)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

### Rules

- **All dates exchanged with the backend are ISO-8601 UTC strings.** Convert at the boundary.
- **All dates in domain models are `LocalDateTime` / `LocalDate` / `Duration` / `Instant`.** No `java.util.Date`, no `Long` epoch.
- **All formatting goes through `DateTimeUtils.format(...)`.** No `SimpleDateFormat` ever.
- **`DateFormatting.install(localeTag)` is called from `RootComponent.Render()` whenever the system locale changes.**
- **Range pickers expose `DateRangeKind`.** The kind is the source of truth; the range is computed from it.

### Anti-patterns

- **`java.util.Date`, `SimpleDateFormat`, `Calendar`.** Forbidden — Java types don't work on iOS.
- **`System.currentTimeMillis()` / `Date()`.** Use `DateTimeUtils.now()`.
- **Inline `LocalDateTime.format(...)` with custom patterns.** Use a typed `DateFormat.DateOnly.*` / `DateFormat.TimeOnly.*`.
- **Storing date as `String` in domain.** Use `LocalDateTime`. Strings are for DTOs and DB entities only.
- **Hardcoded month names.** Use `DateFormat.DateOnly.MonthFull` / `MonthShort`.
- **Forgetting `DateFormatting.install(localeTag)`.** Dates render in the wrong language after system locale change.
- **Different ISO conventions in different DTOs.** Always `toUtcIso(...)` for backend.
