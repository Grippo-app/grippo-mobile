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
| `:toolkit:permission-manager` | System permission requests | `PermissionManager`, `Permission` |
| `:toolkit:link-opener` | Open URLs in system browser | `LinkOpener.open(url: String)` |
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

Provides a base Ktor `HttpClient` (the platform-specific engine), with the `responseValidator` configured to convert HTTP errors into `AppError`. The full client (with Auth, Logging, ContentNegotiation) is built in `:data-services:backend`/`BackendClient` by adding to this base via `client.config { ... }`.

```kotlin
@Module
@ComponentScan
public class HttpModule {
    @Single
    internal fun provideClient(): HttpClient = HttpClient(<Engine>) {
        expectSuccess = false
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.isSuccess()) return@validateResponse
                throw ApiErrorParser.parse(response)
            }
        }
    }
}
```

Platform engine: `Android` on `androidMain` (`io.ktor:ktor-client-android`), `Darwin` on `iosMain` (`io.ktor:ktor-client-darwin`).

## `:toolkit:serialization`

```kotlin
@Module
@ComponentScan
public class SerializationModule {
    @Single
    internal fun provideJson(): Json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}
```

`ignoreUnknownKeys = true` is important — it lets the app survive backend adding new fields without a coordinated release.

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

File sink:
- Android: `<filesDir>/grippo/logs/app.log` (or similar — implementation-specific).
- iOS: `NSTemporaryDirectory()/grippo/logs/`.

`AppLogger.logFileContentsByCategory()` is used by the debug screen to display the rolling log.

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
    public data object ChangeWeight : NotificationKey(1)
    public data object FinishWorkout : NotificationKey(2)
    public data class Custom(override val key: Int) : NotificationKey(key)
}

public interface NotificationManager {
    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): NotificationKey
    public fun cancel(id: NotificationKey)
    public suspend fun isPending(id: NotificationKey): Boolean
}
```

Android: WorkManager / AlarmManager wrapper. iOS: `UNUserNotificationCenter`.

Replace the product-specific `NotificationKey` subtypes per product.

## `:toolkit:permission-manager`

Wraps system permission requests behind a multiplatform interface.

```kotlin
public interface PermissionManager {
    public suspend fun request(permission: Permission): PermissionResult
    public suspend fun isGranted(permission: Permission): Boolean
}

public enum class Permission { Notifications, Camera, /* ... */ }
```

## `:toolkit:link-opener`

```kotlin
public interface LinkOpener {
    public fun open(url: String)
}
```

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
- **No business logic.** A toolkit module is utility-only. No "Training" types, no "User" types.
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
