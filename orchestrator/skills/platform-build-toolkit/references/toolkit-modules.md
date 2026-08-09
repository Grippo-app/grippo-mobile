# Toolkit modules — layout, build-logic, overview

Self-contained reference for the `:toolkit:*` platform-aware utility layer, the `:build-logic` convention-plugin modules, and the toolkit overview (shape, patterns, rules, anti-patterns).

## Toolkit module layout & per-module build specifics

The platform-aware utility layer. Toolkit modules are the **bottom** of the dependency graph: they depend only on each other and on a narrow set of allowed non-toolkit modules (`:ui-core:error:error-provider` for typed errors, `:design-system:resources:provider` + `:design-system:core` for locale-aware resources). UI, data features, and design system all sit above them.

### Module list

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

### `:toolkit:context`

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

### `:toolkit:http-client`

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
- `responseValidator(apiErrorParser)` (in `internal/ResponseValidator.kt`) wires `ApiErrorParser` into Ktor's `HttpResponseValidator { validateResponse / handleResponseExceptionWithRequest }`, mapping HTTP 4xx/5xx and transport faults to `AppError` subtypes from `:ui-core:error:error-provider` (`AppError.Network.Expected`, `Unexpected`, `Timeout`, `NoInternet`). See the data-layer skill, references/error-pipeline.md.

### `:toolkit:serialization`

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
- `prettyPrint = true` — printed JSON (logs, debug screen) is human-readable. Request bodies serialized by Ktor's `ContentNegotiation` will also be pretty-printed when this instance is used; this is acceptable for a debug/development build profile.

### `:toolkit:logger`

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

### `:toolkit:date-utils`

The full date/time toolkit. Highlights:

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
- `DateFormatting.install(localeTag: String?)` — switches all formatters to a new locale; called from `RootComponent.Render()` via `LaunchedEffect(systemLocaleTag)`.

### `:toolkit:theme` / `:toolkit:localization`

`AppTheme.current` (`@Composable expect val Boolean`) — system dark/light. Android: reads `LocalConfiguration.current.uiMode`. iOS: reads `LocalSystemTheme`.

`AppLocale.current` (`@Composable expect val String`) — BCP-47 language tag of the current system locale. Used in `BackendClient.defaultRequest` for `Accept-Language`.

Each also has a non-Composable `current()` overload for usage outside `@Composable` contexts.

### `:toolkit:connectivity`

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

### `:toolkit:notification-manager`

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

### `:toolkit:permission-manager`

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

`check` returns the current status without showing a system dialog; `request` shows the dialog (when applicable) and resolves to the same `PermissionStatus`. Add new entries to `AppPermission` as features need them — the template currently only ships `Notifications`.

### `:toolkit:link-opener`

```kotlin
public interface LinkOpener {
    public fun open(url: String): LinkOpenResult
}
```

`LinkOpenResult` is a sealed result type (success / no-handler / failure) so callers can react to "no app can handle this URL" without try/catch.

Android: `Intent(Intent.ACTION_VIEW, Uri.parse(url))`. iOS: `UIApplication.sharedApplication.openURL(url)`.

### `:toolkit:image-loader`

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

The Coil singleton is installed DI-driven on both platforms — an `@Single(createdAtStart = true)` `ImageLoaderInitializer` calls `SingletonImageLoader.setSafe(...)` during `startKoin`; there is no manual install in the app shells (see `toolkit-utilities-platform.md` § `:toolkit:image-loader`).

### Rules

- **Toolkit modules MUST NOT depend on each other gratuitously.** Each module declares only what it genuinely needs. `:toolkit:logger` is the most-imported toolkit module; most others depend on it.
- **No business logic.** A toolkit module is utility-only. No domain types (e.g. no `Note` types, no `User` types).
- **`expect/actual` over interfaces+impls** for platform-specific behavior. Use interfaces only when the platform-specific impl is non-trivial (e.g. `Connectivity`'s platform implementations differ significantly).
- **Allowed non-toolkit dependencies are narrow:**
  - `:ui-core:error:error-provider` — `:toolkit:http-client` imports `AppError` so the response validator can throw typed errors.
  - `:design-system:resources:provider` + `:design-system:core` — `:toolkit:date-utils` imports them to resolve locale-aware date/time `Res.string.*` formats and theme-derived tokens.

  All three exception targets are pure-type modules (no UI, no DI, no state). Do not widen this list without a deliberate review.

### Build (representative)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    // optional: compose.multiplatform.convention if the module exposes @Composable functions
    // optional: koin.annotation.convention if the module declares a Koin module
    // optional: alias(libs.plugins.kotlin.serialization) if the module declares @Serializable types
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.<name>" }

    sourceSets.commonMain.dependencies {
        // minimal — only the libraries the toolkit module wraps
    }
}
```

### Per-module build specifics

Three toolkit modules need extra plugins beyond the representative block above. Use the templates below verbatim.

#### `:toolkit:serialization`

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
        implementation(libs.koin.core)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

The `kotlin.serialization` plugin alias is mandatory — `SerializationModule` provides a `Json` instance, but the module's own DI/configuration objects use `@Serializable` annotations via the plugin's compiler support.

#### `:toolkit:http-client`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.http.client" }

    sourceSets.commonMain.dependencies {
        implementation(libs.koin.core)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.kotlinx.serialization.json)

        implementation(projects.toolkit.context)
        implementation(projects.toolkit.serialization)
        implementation(projects.uiCore.error.errorProvider)   // ApiErrorParser throws AppError.Network.*
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.ktor.client.android)
    }

    sourceSets.iosMain.dependencies {
        implementation(libs.ktor.client.darwin)
    }
}
```

The `:ui-core:error:error-provider` dependency is the documented back-edge carve-out (see `module-structure.md` § "Dependency rules"). The `kotlin.serialization` plugin is required for `ApiErrorBody` and any future Ktor `ContentNegotiation` payload types declared inside this module.

#### `:toolkit:date-utils`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")    // formatters expose @Composable date-string accessors
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.date.utils" }

    sourceSets.commonMain.dependencies {
        implementation(libs.datetime)
        implementation(libs.kotlinx.serialization.json)

        implementation(projects.designSystem.resources.provider)   // Res.string.* for locale-aware labels
        implementation(projects.designSystem.core)                // AppTokens-derived format hints
        implementation(projects.toolkit.logger)

        implementation(compose.foundation)
    }
}
```

Both back-edges (`:design-system:resources:provider` + `:design-system:core`) are documented in the Rules section above. The `compose.multiplatform.convention` is required because `DateFormat` exposes Composable accessors for locale-bound strings.

## build-logic / convention-plugin modules

`:build-logic` is an **included build** (`includeBuild("build-logic")` in `settings.gradle.kts`'s `pluginManagement`). It hosts all Gradle convention plugins. Module-level `build.gradle.kts` files contain only `plugins { id("...convention") }` and a `kotlin { sourceSets ... }` block — every actual configuration lives in a convention plugin.

This keeps build configuration **DRY** and **consistent**: bumping `compileSdk`, changing JVM toolchain, or adding a global `optIn` is a single edit.

### Structure

```
build-logic/
  settings.gradle.kts                       // includes :convention, points at libs.versions.toml
  build.gradle.kts                          // empty (root)
  convention/
    build.gradle.kts                        // declares the plugins via gradlePlugin {}
    src/main/kotlin/
      KotlinMultiplatformConventionPlugin.kt
      AndroidLibraryConventionPlugin.kt
      AndroidApplicationConventionPlugin.kt
      ComposeMultiplatformConventionPlugin.kt
      KoinAnnotationConventionPlugin.kt
      RoomConventionPlugin.kt
      IosSwiftPackageConventionPlugin.kt
      KmpTestConventionPlugin.kt              // opt-in base test foundation — single androidHostTest owner
      CoroutinesTestConventionPlugin.kt       // capability: kotlinx-coroutines-test in commonTest
      FlowTestConventionPlugin.kt             // capability: Turbine (applies coroutines.test.convention)
      NetworkTestConventionPlugin.kt          // capability: ktor-client-mock in commonTest
      DiTestConventionPlugin.kt               // capability: koin-test on androidHostTest
      RoomTestConventionPlugin.kt             // capability: room-testing + device lane (internal enabler helper)
      ComposeUiTestConventionPlugin.kt        // capability: shared uiTest scenario tree + platform entries
      CoverageTestConventionPlugin.kt         // optional: Kover jvm-host-coverage ratchet
      ScreenshotTestConventionPlugin.kt       // optional, inert — screenshot-fidelity gate (see the implement-figma skill)
      TestCapabilityEntryTask.kt              // per-module inventory fragment task (aggregated at root)
      com/<org>/                            // shared helpers (rename "<org>" per product)
        ConfigureJvmToolchain.kt            // Project.configureJvmToolchain(version)
        PluginManagerExtensions.kt          // PluginManager.applySafely(id)
        ProjectExtensions.kt                // Project.libs accessor
```

### `build-logic/settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

This re-imports the **same** version catalog used by the main build, so convention plugins reference exactly the same versions as `settings.gradle.kts` does.

### `build-logic/convention/build.gradle.kts`

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.ksp.plugin.api)
    // Optional — screenshot-fidelity gate; inert until a module applies `screenshot.test.convention` (see `convention-plugins.md` § "Optional convention plugin").
    implementation(libs.roborazzi.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("android.library.convention") {
            id = "android.library.convention"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("android.application.convention") {
            id = "android.application.convention"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("compose.multiplatform.convention") {
            id = "compose.multiplatform.convention"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("kotlin.multiplatform.convention") {
            id = "kotlin.multiplatform.convention"
            implementationClass = "KotlinMultiplatformConventionPlugin"
        }
        register("koin.annotation.convention") {
            id = "koin.annotation.convention"
            implementationClass = "KoinAnnotationConventionPlugin"
        }
        register("room.convention") {
            id = "room.convention"
            implementationClass = "RoomConventionPlugin"
        }
        register("ios.swiftpackage.convention") {
            id = "ios.swiftpackage.convention"
            implementationClass = "IosSwiftPackageConventionPlugin"
        }
        // Opt-in general test foundation — registered always (ships inert);
        // applied only by test-bearing modules, never transitively.
        register("kmp.test.convention") {
            id = "kmp.test.convention"
            implementationClass = "KmpTestConventionPlugin"
        }
        register("coroutines.test.convention") {
            id = "coroutines.test.convention"
            implementationClass = "CoroutinesTestConventionPlugin"
        }
        register("flow.test.convention") {
            id = "flow.test.convention"
            implementationClass = "FlowTestConventionPlugin"
        }
        register("network.test.convention") {
            id = "network.test.convention"
            implementationClass = "NetworkTestConventionPlugin"
        }
        register("di.test.convention") {
            id = "di.test.convention"
            implementationClass = "DiTestConventionPlugin"
        }
        register("room.test.convention") {
            id = "room.test.convention"
            implementationClass = "RoomTestConventionPlugin"
        }
        register("compose.ui.test.convention") {
            id = "compose.ui.test.convention"
            implementationClass = "ComposeUiTestConventionPlugin"
        }
        register("coverage.test.convention") {
            id = "coverage.test.convention"
            implementationClass = "CoverageTestConventionPlugin"
        }
        // Optional — registered always (ships inert); applied only by feature modules in the screenshot gate.
        register("screenshot.test.convention") {
            id = "screenshot.test.convention"
            implementationClass = "ScreenshotTestConventionPlugin"
        }
    }
}
```

See `convention-plugins.md` for the verbatim body of every plugin.

### Rules

1. **One convention plugin per concern.** Don't merge "android library" and "compose" into one. The matrix is documented in `convention-plugins.md`.
2. **`applySafely`** — use `pluginManager.applySafely("com.foo.bar")` instead of `pluginManager.apply("com.foo.bar")`. The helper checks `hasPlugin(...)` first; multiple convention plugins may pull in the same upstream plugin and this avoids errors.
3. **Configuration cache compatible.** No `project.evaluationDependsOn(...)`. No eager access to other projects' configurations. Use `pluginManager.withPlugin("...") { extensions.configure<...> { ... } }` to defer configuration until the upstream plugin is applied.
4. **No conditional logic per project.** If a module needs different settings, it must consume a different convention plugin. Don't read `project.name` and branch.

### When to add a new convention plugin

Add when:

- A concern touches **multiple** modules with the **same** settings. (E.g. if every screen feature module needs the same Compose stability config beyond what `ComposeMultiplatformConventionPlugin` provides.)
- A convention plugin's behavior would become heavily conditional. Split it.

Don't add for:

- A single module's special needs — put the config in that module's `build.gradle.kts`.
- One-off plugin applications.

### Module-level `build.gradle.kts` shape

After convention plugins do their work, every module's `build.gradle.kts` shrinks to:

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")    // if the module has Compose UI
    id("koin.annotation.convention")          // if the module declares a Koin module
    id("room.convention")                     // only :data-services:database
    alias(libs.plugins.kotlin.serialization)  // if @Serializable types are declared
}

kotlin {
    android { namespace = "com.<org>.<product>.<module-path>" }

    sourceSets.commonMain.dependencies {
        // module-specific dependencies
    }
}
```

`compileSdk`, `minSdk`, `jvmToolchain`, `explicitApi`, `optIn`, KSP wiring, Room schema location — all in convention plugins. Module-level config is intentionally minimal.

## Toolkit overview — shape, patterns, rules

The toolkit holds **platform-aware utilities** sitting at the bottom of the dependency graph. Every other layer (UI, data features, design system) may depend on toolkit modules. The toolkit may only depend on **other toolkit modules**, with two narrow exceptions in the template: `:toolkit:http-client` reads `AppError` from `:ui-core:error:error-provider`, and `:toolkit:date-utils` reads localized weekday/month resources from `:design-system:resources:provider` + `:design-system:core`. No toolkit module depends on `:data-services:*`.

To change this layer — extend an existing module or add a new one — follow `references/toolkit-cookbook.md` (owned by `toolkit-builder`).

### Modules at a glance

| Module | Purpose |
|---|---|
| `:toolkit:context` | `NativeContext` (expect/actual platform handle) |
| `:toolkit:http-client` | Base Ktor `HttpClient` + `ApiErrorParser` |
| `:toolkit:serialization` | `Json` provider |
| `:toolkit:logger` | `AppLogger` (file-backed, categorized) |
| `:toolkit:date-utils` | `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting` |
| `:toolkit:theme` | `AppTheme.current` — system dark/light theme |
| `:toolkit:localization` | `AppLocale.current` — system BCP-47 language tag |
| `:toolkit:connectivity` | `Connectivity.statusUpdates` — online/offline |
| `:toolkit:notification-manager` | Local notifications (`AppNotification`, `NotificationKey`) |
| `:toolkit:permission-manager` | System permission requests |
| `:toolkit:link-opener` | Open URLs in system browser |
| `:toolkit:image-loader` | Coil 3 + Ktor 3 image loader |

### Shape of a toolkit module

```
:toolkit:<name>/
  build.gradle.kts                  // KMP convention + Koin if it declares a module
  src/
    commonMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.kt                  // public interface or expect class
      <Service>Module.kt             // public Koin module (optional)
      internal/                      // private impls (convention only)
    androidMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.android.kt           // actual or impl class
    iosMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.ios.kt
```

### When to use `expect/actual` vs `interface + per-platform impl`

| Choice | When |
|---|---|
| `expect/actual` | Small helpers, single-method classes (e.g. `NativeContext`, `AppTheme.current`, `AppLocale.current`). Type is the same on both platforms; only the body differs. |
| `interface + Android impl + iOS impl` | Large services with multiple methods and meaningful state (e.g. `Connectivity`, `NotificationManager`, `PermissionManager`). |

This template uses a mix; for the new project, pick the right tool per service.

### Common patterns

#### Empty iOS implementation

When a service is Android-only by intent (e.g. Firebase Analytics is set up natively on iOS in Swift), expose an interface in `commonMain` and implement it as a no-op on iOS:

```kotlin
// commonMain
public interface FirebaseAnalyticsService {
    public fun logEvent(name: String, params: Map<String, String>)
}

// iosMain
internal class IosFirebaseAnalytics : FirebaseAnalyticsService {
    override fun logEvent(name: String, params: Map<String, String>) {
        // no-op; iOS uses native SDK
    }
}
```

#### Lazy platform setup

```kotlin
// :toolkit:notification-manager
public interface NotificationManager {
    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): NotificationKey
    public fun cancel(id: NotificationKey)
    public suspend fun isPending(id: NotificationKey): Boolean
}
```

Implementation registers the channel/notification center lazily on first `show(...)`. Avoids cold-start cost.

#### File-backed services

`AppLogger` writes to an append-only file. `:toolkit:logger` keeps the writer behind an internal `expect class`:

```kotlin
internal expect class LogFileWriter {
    val location: String
    fun append(text: String)

    companion object {
        fun create(fileName: String = DEFAULT_FILE_NAME): LogFileWriter
        fun deleteFile(path: String): Boolean
        val DEFAULT_FILE_NAME: String
    }
}
```

The platform `actual` resolves the base directory inside a private `resolveBaseDirectory()` helper (no separate `LogFileLocation` type) and returns a writer pointed at `<baseDir>/<fileName>`. The `LogDispatcher` in `commonMain` decides which category each entry belongs to and asks the writer to `append(text)` — the writer itself is category-agnostic.

### Rules

1. **No business logic.** Toolkit is utility-only. No domain types (`Note`, `Tag`).
2. **No design-system dependency** (beyond the two narrow carve-outs in the intro). Toolkit doesn't read `AppTokens`.
3. **No data-feature dependency.** Toolkit doesn't import `<X>Feature`.
4. **No cross-feature toolkit imports.** `:toolkit:notification-manager` doesn't import `:toolkit:date-utils` unless it really needs to.
5. **Public interfaces** — every consumer-facing type is `public`. Implementations are `internal`.
6. **Koin module per service** — `<Name>Module` registered in `:shared/Koin.kt`.

### When to create a new toolkit module

A new module is justified when:

- The service has platform-specific impls.
- The service is consumed by **multiple** layers (UI + data + screen).
- The service is **infrastructure** (timing, IO, system services) not business logic.

Don't create a toolkit module for:

- A single feature's helper. Put it in the feature's module.
- A wrapper around a single Kotlin stdlib feature. Use stdlib directly.
- A "string utilities" module. Use Kotlin's stdlib extensions.

### Where each toolkit module sits in the graph

```
:design-system:* / :ui-core:* / :data-features:* / :data-services:*
        ↓ ↓ ↓ ↓
:toolkit:*           (depends only on other :toolkit:* — plus the two narrow
                      exceptions noted above: :toolkit:http-client uses
                      :ui-core:error:error-provider; :toolkit:date-utils uses
                      :design-system:core + :design-system:resources:provider)
        ↓
(kotlinx stdlib, Ktor, Room, Coil, etc.)
```

A toolkit module's `build.gradle.kts` typically imports a few sibling `:toolkit:*` modules and the library it wraps.

### Anti-patterns

- **Toolkit module that depends on `:design-system:*`.** Forbidden without a documented exception.
- **Toolkit module that depends on `:data-features:*`.** Forbidden.
- **Toolkit module that depends on `:ui-core:*`.** Forbidden without a documented exception.
- **Toolkit module with `@Composable` functions** that read `AppTokens`. Compose Composables in a toolkit module is fine; reading design tokens is not.
- **Concrete platform classes in `commonMain`.** Use `expect/actual` or `interface + per-platform impl`.
- **Static accessors in `commonMain`** for things that vary by platform (`Context`, `NSBundle`). Wrap behind `NativeContext` from `:toolkit:context`.
