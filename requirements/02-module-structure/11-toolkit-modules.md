# `:toolkit:*` Modules

The platform-aware utility layer. Toolkit modules are the **bottom** of the dependency graph: they depend only on each other (and on `:data-services:firebase` for crash logging). UI, data features, and design system all sit above them.

## Module list

| Module | Purpose | Key types |
|---|---|---|
| `:toolkit:context` | Multiplatform context handle | `NativeContext` (expect/actual), `ContextModule` |
| `:toolkit:http-client` | Ktor `HttpClient` provider + error parser | `HttpModule`, `ApiErrorParser`, `responseValidator` |
| `:toolkit:serialization` | `Json` provider | `Json` (lenient, ignoreUnknownKeys), `SerializationModule` |
| `:toolkit:logger` | File-backed logger | `AppLogger` (`General`, `Navigation`, `Network`, `Mapping`) |
| `:toolkit:date-utils` | Date/time toolkit | `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting` |
| `:toolkit:theme` | System dark/light theme detection | `AppTheme.current` (`@Composable` expect) |
| `:toolkit:localization` | System locale | `AppLocale.current` (`@Composable` expect) |
| `:toolkit:connectivity` | Online/offline `SharedFlow` | `Connectivity`, `Connectivity.Status`, `ConnectivityOptions` |
| `:toolkit:notification-manager` | Local notifications | `NotificationManager`, `AppNotification`, `NotificationKey` |
| `:toolkit:permission-manager` | System permission requests | `PermissionManager`, `AppPermission`, `PermissionStatus` |
| `:toolkit:link-opener` | Open URLs in system browser | `LinkOpener.open(url: String): LinkOpenResult` |
| `:toolkit:image-loader` | Coil setup | `ImageLoaderModule` (Coil 3 + Ktor) |

Each module exposes a Koin `<X>Module` (or is included transitively) so its services are wired into the composition.

## `:toolkit:context`

```kotlin
// commonMain
public expect class NativeContext

// androidMain
public actual class NativeContext(public val context: Context)

// iosMain
public actual class NativeContext

@Module
@ComponentScan
public expect class ContextModule() {
    @Single
    internal fun providesNativeContext(scope: Scope): NativeContext
}

// androidMain
public actual class ContextModule actual constructor() {
    @Single
    internal actual fun providesNativeContext(scope: Scope): NativeContext =
        NativeContext(scope.get<Application>())
}

// iosMain
public actual class ContextModule actual constructor() {
    @Single
    internal actual fun providesNativeContext(scope: Scope): NativeContext =
        NativeContext()
}
```

Note: `ContextModule` is itself `expect/actual` because Android needs to pull the `Application` instance from Koin (registered by `androidContext(this@App)`), while iOS just constructs an empty object. The Android resolver requests `Application` rather than `Context` so we always get the application-scoped instance, never a leaking `Activity`.

## `:toolkit:http-client`

Provides a base Ktor `HttpClient` whose engine and `responseValidator` are pre-installed, leaving Auth / Logging / ContentNegotiation to `:data-services:backend`'s `BackendClient` (it calls `httpClient.config { ... }` on the injected instance).

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
```

- `NativeContext.driver(): HttpClient` is an `internal expect/actual` factory that constructs the platform engine — `Android` on `androidMain` (`io.ktor:ktor-client-android`), `Darwin` on `iosMain` (`io.ktor:ktor-client-darwin`).
- `responseValidator(apiErrorParser)` (in `internal/ResponseValidator.kt`) wires `ApiErrorParser` into Ktor's `HttpResponseValidator { validateResponse / handleResponseExceptionWithRequest }`, mapping HTTP 4xx/5xx and transport faults to `AppError` subtypes from `:ui-core:error:error-provider` (`AppError.Network.Expected`, `Unexpected`, `Timeout`, `NoInternet`). See `03-architecture-patterns/07-error-pipeline.md`.

## `:toolkit:serialization`

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

- `ignoreUnknownKeys = true` — survive backend adding new fields without a coordinated release.
- `isLenient = true` — accept relaxed JSON (unquoted keys, trailing commas, etc.) so a stray server quirk doesn't crash the parser.
- `useAlternativeNames = false` — skip building the alternative-name index. The codebase always declares the exact wire name with `@SerialName("…")` and never relies on `@JsonNames`, so the index is wasted work.
- `prettyPrint = true` — printed JSON (logs, debug screen) is human-readable. It does not affect on-the-wire payload size (Ktor's `ContentNegotiation` re-serializes via the same `Json` instance, but request bodies are still compact for the network path).

## `:toolkit:logger`

`AppLogger` is a singleton with four categories:

```kotlin
public object AppLogger {
    public fun logFileContentsByCategory(): Map<String, List<String>>
    public fun clearLogFile(): Boolean

    public object General { public fun error(msg: String); public fun warning(msg: String) }
    public object Navigation { public fun log(msg: String) }
    public object Network { public fun log(msg: String) }
    public object Mapping {
        public fun <T> log(value: T?, msg: () -> String): T?
    }
}
```

File sink: a single append-only `app.log` written to a product-scoped directory under the user's home (`${user.home}/<product>/logs/app.log` on Android; falls back to `java.io.tmpdir` / `/tmp` if `user.home` is unset) and the iOS equivalent inside `NSTemporaryDirectory()`. There is no rotation — callers reset the file via `AppLogger.clearLogFile()`.

`AppLogger.logFileContentsByCategory()` is used by the debug screen to read the file back, parsing each line into its category prefix.

## `:toolkit:date-utils`

The full date/time toolkit. See `10-toolkit/06-date-utils.md` for full API. Highlights:

- `DateTimeUtils.now()` — `LocalDateTime` in system zone.
- `DateTimeUtils.toUtcIso(LocalDateTime): String` — ISO-8601 UTC string for backend.
- `DateTimeUtils.toLocalDateTime(timestamp: String): LocalDateTime` — parse.
- `DateTimeUtils.format(value, format)` — three overloads for `LocalDateTime`, `LocalDate`, `LocalTime`.
- `DateTimeUtils.format(duration: Duration): String`.
- Range factories: `thisDay`, `thisWeek`, `thisMonth`, `thisYear`, `trailingYear`, `trailing60Days`, `trailingMonth`, `trailing14Days`, `trailingWeek`, `leadingYear`, `infinity`.
- Predicates: `isToday`, `isYesterday`, `isTomorrow`, `isPast`, `isFuture`.
- `DateRange(from, to)` (`@Serializable`) with `.coerceWithin(limitations)` and `.isWellFormed()`.
- `DateRangeKind` enum (`Daily, Weekly, Last7Days, Last14Days, Monthly, Last30Days, Last60Days, Last365Days, Yearly, Infinity, Custom`).
- `DateRangePresets.resolve(kind: DateRangeKind): DateRange?` / `.classify(range: DateRange): DateRangeKind`.
- `DateFormat` sealed interface with `DateOnly`, `TimeOnly`, `DateTime` subtypes (e.g. `DateMmmDdYyyy`, `MmmYyyy`, `Time24hHm`, ...).
- `DateFormatting.install(localeTag: String?)` — switches all formatters to a new locale; called from `RootScreen` via `LaunchedEffect(systemLocaleTag)`.

## `:toolkit:theme` / `:toolkit:localization`

`AppTheme.current` (`@Composable expect val Boolean`) — system dark/light. Android: reads `LocalConfiguration.current.uiMode`. iOS: reads `LocalSystemTheme`.

`AppLocale.current` (`@Composable expect val String`) — BCP-47 language tag of the current system locale. Used in `BackendClient.defaultRequest` for `Accept-Language`.

Each also has a non-Composable `current()` overload for usage outside `@Composable` contexts.

## `:toolkit:connectivity`

```kotlin
public interface Connectivity {
    public val statusUpdates: SharedFlow<Status>
    public val monitoring: StateFlow<Boolean>
    public suspend fun status(): Status
    public fun start()
    public fun stop()

    public sealed interface Status {
        public data class Connected(val metered: Boolean) : Status
        public data object Disconnected : Status
    }
}

public open class ConnectivityOptions(public val autoStart: Boolean = false)
```

`ConnectivityModule` wires the platform-specific implementation. With `autoStart = true`, `statusUpdates` begins emitting immediately on Koin start.

## `:toolkit:notification-manager`

```kotlin
public data class AppNotification(
    val id: NotificationKey,
    val title: String,
    val body: String,
    val deeplink: String? = null,
)

public sealed class NotificationKey(public open val key: Int) {
    // Add product-specific subtypes here, e.g.:
    //   public data object <SomeReminder> : NotificationKey(1)
    public data class Custom(override val key: Int) : NotificationKey(key)
}

public interface NotificationManager {
    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): NotificationKey
    public fun cancel(id: NotificationKey)
    public suspend fun isPending(id: NotificationKey): Boolean
}
```

Android: WorkManager / AlarmManager wrapper. iOS: `UNUserNotificationCenter`.

The Android module also enables `androidLibrary { androidResources.enable = true }` in its `build.gradle.kts` so it can ship the launcher / status-bar icon drawable used by the notification builder. Mirror that opt-in if your product adds notification-only resources.

Replace the product-specific `NotificationKey` subtypes per product.

## `:toolkit:permission-manager`

Wraps system permission requests behind a multiplatform interface.

```kotlin
public interface PermissionManager {
    public suspend fun check(permission: AppPermission): PermissionStatus
    public suspend fun request(permission: AppPermission): PermissionStatus
}

public enum class AppPermission { Notifications }

public sealed class PermissionStatus {
    public data object Granted : PermissionStatus()
    public data object Denied : PermissionStatus()
    public data object DeniedPermanently : PermissionStatus()
}
```

`check` returns the current status without showing a system dialog; `request` shows the dialog (when applicable) and resolves to the same `PermissionStatus`. Add new entries to `AppPermission` as features need them — the reference repo currently only ships `Notifications`.

## `:toolkit:link-opener`

```kotlin
public interface LinkOpener {
    public fun open(url: String): LinkOpenResult
}
```

`LinkOpenResult` is a sealed result type (success / no-handler / failure) so callers can react to "no app can handle this URL" without try/catch.

Android: `Intent(Intent.ACTION_VIEW, Uri.parse(url))`. iOS: `UIApplication.sharedApplication.openURL(url)`.

## `:toolkit:image-loader`

Coil 3 + Ktor 3 setup. Builds the `ImageLoader` with Ktor as the network layer (so the auth-aware Ktor client can serve image requests too, if needed).

```kotlin
@Module
@ComponentScan
public class ImageLoaderModule {
    @Single
    internal fun provideImageLoader(nativeContext: NativeContext, httpClient: HttpClient): ImageLoader {
        return ImageLoader.Builder(nativeContext)
            .components { add(KtorNetworkFetcherFactory(httpClient)) }
            .build()
    }
}
```

The Coil singleton is installed in `App.onCreate` on Android and in the iOS entry point.

## Rules

- **Toolkit modules MUST NOT depend on each other gratuitously.** Each module declares only what it genuinely needs. `:toolkit:logger` is the most-imported toolkit module; most others depend on it.
- **No business logic.** A toolkit module is utility-only. No domain types (e.g. no `<Note>` types, no `User` types).
- **`expect/actual` over interfaces+impls** for platform-specific behavior. Use interfaces only when the platform-specific impl is non-trivial (e.g. `Connectivity`'s platform implementations differ significantly).
- **Allowed non-toolkit dependencies are narrow:**
  - `:data-services:firebase` — for the error pipeline (`FirebaseCrashlytics.recordException(...)`).
  - `:ui-core:error:error-provider` — `:toolkit:http-client` imports `AppError` so the response validator can throw typed errors.
  - `:design-system:resources:provider` + `:design-system:core` — `:toolkit:date-utils` imports them to resolve locale-aware date/time `Res.string.*` formats and theme-derived tokens.

  All three exception targets are pure-type modules (no UI, no DI, no state). Do not widen this list without a deliberate review.

## Build (representative)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    // optional: compose.multiplatform.convention if the module exposes @Composable functions
    // optional: koin.annotation.convention if the module declares a Koin module
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.<name>" }

    sourceSets.commonMain.dependencies {
        // minimal — only the libraries the toolkit module wraps
    }
}
```
