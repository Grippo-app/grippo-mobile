# Toolkit utilities (platform services) — connectivity, notification-manager, permission-manager, link-opener, theme, localization, image-loader

Per-module specifications for the platform-service `:toolkit:*` modules: API surfaces, options, usage, build configuration, Koin wiring, rules, and anti-patterns. Self-contained reference — reads no external rule docs at runtime.

## :toolkit:connectivity

### API

```kotlin
public interface Connectivity {
    public val statusUpdates: SharedFlow<Status>
    public val monitoring: StateFlow<Boolean>

    public val isMonitoring: Boolean
        get() = monitoring.value

    public suspend fun status(): Status
    public fun start()
    public fun stop()

    public sealed interface Status {
        public val isConnected: Boolean
            get() = this is Connected
        public val isMetered: Boolean
            get() = this is Connected && metered
        public val isDisconnected: Boolean
            get() = this is Disconnected

        public data class Connected(public val metered: Boolean) : Status
        public data object Disconnected : Status
    }
}
```

### Options

```kotlin
public open class ConnectivityOptions(
    public val autoStart: Boolean = DEFAULT_AUTO_START,
) {
    public class Builder internal constructor() {
        public var autoStart: Boolean = DEFAULT_AUTO_START
        public fun autoStart(autoStart: Boolean): Builder
        public fun build(): ConnectivityOptions
    }

    public companion object {
        private const val DEFAULT_AUTO_START: Boolean = false
        public fun build(block: Builder.() -> Unit): ConnectivityOptions
    }
}
```

`autoStart = true` means `Connectivity` begins monitoring as soon as it's created (typically at Koin start). With `autoStart = false`, the consumer must call `start()` explicitly.

This template configures `autoStart = true` in `ConnectivityModule`.

### Status

| Status | Meaning |
|---|---|
| `Connected(metered = false)` | Online, Wi-Fi |
| `Connected(metered = true)` | Online, cellular (paid bandwidth) |
| `Disconnected` | No network |

`metered` matters for product decisions: don't auto-sync large blobs on cellular without user consent.

### `statusUpdates` semantics

- **`SharedFlow<Status>`** with `replay = 1` and `BufferOverflow.DROP_OLDEST`. The most recent status is replayed to new subscribers; older statuses are dropped if no consumer is ready.
- **First emission** is the current status at subscription time.
- **No de-duplication.** `DefaultConnectivity` forwards every emission from the platform provider — Android's `onCapabilitiesChanged` can re-emit the same `Connected(...)` value. Apply `distinctUntilChanged()` at the call site if duplicates matter.

### Usage

#### Observing in `RootViewModel`

```kotlin
init {
    connectivity.statusUpdates
        .onEach(::provideConnectionStatus)
        .safeLaunch()
}

private fun provideConnectionStatus(status: Connectivity.Status) {
    update { it.copy(isOnline = status.isConnected) }
}
```

`RootState` exposes `isOnline: Boolean`; a global "offline" banner can read this.

#### Conditional fetching

```kotlin
internal class FooViewModel(
    private val connectivity: Connectivity,
    private val feature: BarFeature,
) : BaseViewModel<...>(...) {

    override fun onRefreshClick() {
        if (connectivity.statusUpdates.replayCache.firstOrNull()?.isDisconnected == true) {
            // skip the fetch; show stale-banner
            return
        }
        safeLaunch(loader = FooLoader.Fetch) {
            feature.fetch().getOrThrow()
        }
    }
}
```

For most cases, **don't gate calls on connectivity** — let them fail and surface `AppError.Network.NoInternet` via the error pipeline. The error dialog already explains "No internet". Gating before the call adds complexity without benefit.

Connectivity-based gating is justified for **large** operations (uploads, video, downloads) where the user shouldn't be surprised by a partial result.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.connectivity" }

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(projects.toolkit.context)
    }
}
```

### Koin module

```kotlin
@Module(includes = [ContextModule::class])
@ComponentScan
public class ConnectivityModule {
    @Single
    internal fun provideConnectivity(nativeContext: NativeContext): Connectivity =
        nativeContext.createConnectivity()
}

// commonMain — internal/NativeConnectivity.kt
internal fun NativeContext.createConnectivity(
    provider: ConnectivityProvider = getConnectivityProvider(),
    options: ConnectivityOptions = ConnectivityOptions(autoStart = true),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
): Connectivity = DefaultConnectivity(scope, provider, options)

// commonMain — internal/ConnectivityProviderFactory.kt
internal expect fun NativeContext.getConnectivityProvider(): ConnectivityProvider
```

`createConnectivity` is a regular extension with defaults — `autoStart = true` is baked into the default `ConnectivityOptions`, so the module body stays one-line. The actual `expect/actual` boundary is `getConnectivityProvider()` (Android: `AndroidConnectivityProvider(context)`; iOS: `AppleConnectivityProvider()`).

### Anti-patterns

- **Gating every API call on connectivity.** The error pipeline already handles offline; double-gating is duplicate logic.
- **Storing connection status in multiple ViewModels.** Centralize in `RootViewModel.state.isOnline`; other VMs read from it via the user-facing banner.
- **Manually polling the network.** Use `statusUpdates`.
- **Subscribing to `statusUpdates` outside a `safeLaunch`.** Lifecycle management bug.

## :toolkit:notification-manager

### API

```kotlin
public interface NotificationManager {
    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): NotificationKey
    public fun cancel(id: NotificationKey)
    public suspend fun isPending(id: NotificationKey): Boolean
}

public data class AppNotification(
    val id: NotificationKey,
    val title: String,
    val body: String,
    val deeplink: String? = null,
)

public sealed class NotificationKey(public open val key: Int) {
    public data object NoteReminder : NotificationKey(1)
    public data object NoteArchived : NotificationKey(2)
    public data class Custom(override val key: Int) : NotificationKey(key)
}
```

Replace `NoteReminder`/`NoteArchived` with product-specific notification kinds. The `Custom(Int)` subtype is the escape hatch for dynamic IDs.

### Usage

```kotlin
internal class NoteViewModel(
    private val notificationManager: NotificationManager,
    private val stringProvider: StringProvider,
) : BaseViewModel<...>(...), ... {

    override fun onSaveNoteClick() {
        safeLaunch(loader = NoteLoader.SavingNote) {
            feature.saveNote(...).getOrThrow()
            scheduleReminder()
        }
    }

    private suspend fun scheduleReminder() {
        val notification = AppNotification(
            id = NotificationKey.NoteReminder,
            title = stringProvider.get(Res.string.notification_note_title),
            body = stringProvider.get(Res.string.notification_note_description),
            deeplink = Deeplink.Notes.key,
        )
        notificationManager.show(notification, 7.days)
    }
}
```

The strings come from `StringProvider` (not `AppTokens.strings` — we're in a VM, not a Composable). The deeplink is a key the OS notification carries; tapping the notification opens the app with this deeplink.

### Deeplink handling

When the user taps a notification:

- **Android**: `MainActivity` is launched with an `Intent` containing the deeplink. `onNewIntent` parses it and calls `rootComponent.applyDeeplink(deeplink)`.
- **iOS**: `UNUserNotificationCenterDelegate.didReceiveResponse` extracts the deeplink and passes it to the Kotlin layer via a bridge.

See the di-modules skill, references/composition-root.md for the wiring.

### `NotificationKey`

```kotlin
public sealed class NotificationKey(public open val key: Int) {
    public data object NoteReminder : NotificationKey(1)
    public data object NoteArchived : NotificationKey(2)
    public data class Custom(override val key: Int) : NotificationKey(key)
}
```

- **`Int key`** — Android `NotificationManager.notify(id, ...)` and iOS `UNNotificationRequest`'s identifier both use integers (iOS uses strings; the wrapper converts).
- **`sealed class` with data objects** for known kinds — type-safe + auto-deduplicated.
- **`Custom(Int)`** for dynamic IDs (e.g. one notification per note reminder).

Showing twice with the same key **replaces** the prior notification:

```kotlin
notificationManager.show(AppNotification(id = NotificationKey.NoteReminder, ...), 1.days)
notificationManager.show(AppNotification(id = NotificationKey.NoteReminder, ...), 3.days)
// Only the second is pending; the first is overwritten.
```

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.notification.manager" }

    androidLibrary {
        androidResources.enable = true     // notification icons/strings live in androidMain res
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
    }
}
```

Android implementation uses `AlarmManager` with a `BroadcastReceiver` (`ScheduledNotificationReceiver`) to post the notification at the scheduled time. iOS uses `UNUserNotificationCenter` with `UNTimeIntervalNotificationTrigger`.

### Permissions

- **Android 13+**: `POST_NOTIFICATIONS` runtime permission. Request via `:toolkit:permission-manager`.
- **iOS**: `UNUserNotificationCenter.requestAuthorization(options:)`.

Request permission at a **user-intent moment** (when the user toggles "Remind me to revisit this note"), not on app start. The `PermissionManager` interface handles this.

### Rules

- **`NotificationKey` values are stable** across app versions. Don't renumber existing keys.
- **`NotificationKey.Custom(id)` IDs must not collide** with sealed constants.
- **Strings come from `StringProvider`** (not `AppTokens.strings` — VM is not `@Composable`).
- **`title` and `body` are plain text.** No HTML, no markdown.
- **`deeplink` is a stable string key** (typically `Deeplink.<X>.key`). Don't ship a notification with a deeplink that points to a route you might rename.

### Anti-patterns

- **Pre-localized title/body without `StringProvider`.** The notification won't update if the user changes the system locale.
- **`NotificationKey.Custom(Long)` style.** The key is `Int` for cross-platform compatibility.
- **Showing a notification synchronously without `safeLaunch`.** If the call blocks (rare), the UI thread stalls.
- **Forgetting to request the notification permission.** On Android 13+ and iOS, notifications are silently dropped without permission.
- **Push notifications via this module.** Push goes through Firebase Messaging (Android) / APNs (iOS) — separate path. `:toolkit:notification-manager` is local-only.

## :toolkit:permission-manager

### API

```kotlin
public interface PermissionManager {
    public suspend fun check(permission: AppPermission): PermissionStatus
    public suspend fun request(permission: AppPermission): PermissionStatus
}

public enum class AppPermission {
    Notifications,
    // Add new entries here as features need them.
}

public sealed class PermissionStatus {
    public data object Granted : PermissionStatus()
    public data object Denied : PermissionStatus()
    public data object DeniedPermanently : PermissionStatus()
}
```

- `check(...)` returns the current status without showing the system dialog.
- `request(...)` shows the dialog when status is `Denied`; for `Granted` and `DeniedPermanently` it short-circuits and returns the current status.
- The reference enum has only `Notifications`. Add `Camera`/`Microphone`/`Location` etc. when a feature actually needs them — adding now means each new entry needs Android + iOS impls.

### Usage

```kotlin
internal class NoteViewModel(
    private val permissionManager: PermissionManager,
    private val notificationManager: NotificationManager,
) : BaseViewModel<...>(...) {

    override fun onEnableRemindersClick() {
        safeLaunch {
            when (permissionManager.request(AppPermission.Notifications)) {
                PermissionStatus.Granted -> {
                    update { it.copy(remindersEnabled = true) }
                    scheduleReminder()
                }
                PermissionStatus.Denied -> {
                    // show inline message; user may try again
                }
                PermissionStatus.DeniedPermanently -> {
                    dialogController.show(DialogConfig.OpenSettingsPrompt)
                }
            }
        }
    }
}
```

Request permission **at the moment the user wants the feature**, not at app start. Boot-time permission requests cause permission fatigue.

### `AppPermission.Notifications` Android specifics

- **API 33+ (Android 13)**: `POST_NOTIFICATIONS` is a runtime permission. Request via this interface.
- **API < 33**: notifications work without runtime permission (auto-granted). `check(AppPermission.Notifications)` returns `Granted`.

iOS: `UNUserNotificationCenter.requestAuthorization(...)`. The permission persists across app launches.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.permission.manager" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.context)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)   // ActivityResultContracts.RequestMultiplePermissions
        }
    }
}
```

### Implementation outlines

#### Android

```kotlin
// androidMain
internal class AndroidPermissionManager(
    private val context: Context,
) : PermissionManager {
    override suspend fun check(permission: AppPermission): PermissionStatus { /* checkSelfPermission */ }
    override suspend fun request(permission: AppPermission): PermissionStatus { /* launches PermissionRequestActivity, awaits result via PermissionResultHolder */ }
}
```

The Android impl launches an internal `PermissionRequestActivity` to host the system prompt (the call site doesn't need to be an Activity) and uses a `PermissionResultHolder` to bridge the result back into the coroutine. `DeniedPermanently` is detected by `!shouldShowRequestPermissionRationale(...)` after the user denies.

#### iOS

```kotlin
// iosMain
internal class IosPermissionManager : PermissionManager {
    override suspend fun check(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.Notifications -> currentNotificationStatus()
    }
    override suspend fun request(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.Notifications -> requestNotificationAuthorization()
    }
    // requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionBadge) { granted, _ -> ... }
}
```

### Rules

- **Request at user-intent moments**, never at app start.
- **`check(...)` is non-blocking** — it queries the current status without showing UI.
- **`request(...)` is suspendable** — it shows the system dialog (when applicable) and awaits the user's response.
- **`DeniedPermanently` triggers a "open Settings" prompt** — link via `:toolkit:link-opener`.

### Anti-patterns

- **Requesting all permissions at app start.** Causes immediate uninstalls.
- **`request(...)` without a user-visible reason.** Always pair with a UI message explaining why.
- **Ignoring `DeniedPermanently`.** The user has dismissed the prompt twice; show a Settings deeplink or graceful fallback.
- **Caching `request(...)` results forever.** The user may revoke in Settings; re-check via `check(...)`.
- **Coupling `PermissionManager` to specific features.** Keep it generic; features call it.

## :toolkit:link-opener

### API

```kotlin
public interface LinkOpener {
    public fun open(url: String): LinkOpenResult
}

public data class LinkOpenResult(
    public val isOpened: Boolean,
    public val resolvedHandler: String? = null,
)
```

`isOpened` reports whether the system actually launched a handler; `resolvedHandler` exposes the Android package that resolved the intent (null on iOS). Most callers ignore the result — it's there so analytics / diagnostics can record which app handled a deeplink.

### Implementations

#### Android (`AndroidLinkOpener`)

```kotlin
internal class AndroidLinkOpener(private val context: Context) : LinkOpener {
    override fun open(url: String): LinkOpenResult {
        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?.takeIf { !it.scheme.isNullOrBlank() }
            ?: return LinkOpenResult(isOpened = false)

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = runCatching {
            intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY)?.packageName
        }.getOrNull()

        return runCatching {
            context.startActivity(intent)
            LinkOpenResult(isOpened = true, resolvedHandler = resolved)
        }.getOrNull() ?: LinkOpenResult(isOpened = false)
    }
}
```

The Android impl takes raw `Context` (not `NativeContext`) — the platform `LinkOpenerFactory.android.kt` reaches into `NativeContext.context` and constructs `AndroidLinkOpener(context)`.

#### iOS (`AppleLinkOpener`)

```kotlin
internal class AppleLinkOpener : LinkOpener {
    override fun open(url: String): LinkOpenResult {
        val nsUrl = NSURL(string = url)
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(nsUrl)) return LinkOpenResult(isOpened = false)

        val opened = runCatching {
            app.openURL(url = nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
            true
        }.getOrDefault(false)
        return LinkOpenResult(isOpened = opened)
    }
}
```

### Usage

```kotlin
internal class TermsOfServiceViewModel(
    private val linkOpener: LinkOpener,
) : BaseViewModel<...>(...), TermsOfServiceContract {

    override fun onTermsClick() {
        linkOpener.open("https://<product-domain>.com/terms")
    }
}
```

### Rules

- **Open `https://` URLs only.** No custom schemes, no `tel:` / `mailto:` (those have their own helpers).
- **Treat `LinkOpenResult.isOpened == false` as advisory** — the user pressed a link; if nothing handles it, fall back to a Toast/Snackbar at most. Don't escalate to a full `AppError` dialog.
- The Android impl already drops malformed URIs early (empty/missing scheme returns `LinkOpenResult(isOpened = false)`).

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.link.opener" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
    }
}
```

### Anti-patterns

- **Custom in-app browsers** via `WebView`/`WKWebView`. Use the system browser unless there's a strong reason (e.g. OAuth flows handled in `CustomTabs`).
- **Branching loudly on `LinkOpenResult.isOpened`.** Most call sites can ignore it; reserve it for analytics / diagnostics.

## :toolkit:theme & :toolkit:localization

### `:toolkit:theme` — `AppTheme.current`

```kotlin
// commonMain
public expect object AppTheme {
    public val current: Boolean
        @Composable get

    public fun current(): Boolean
}
```

#### Android

```kotlin
public actual object AppTheme {
    public actual val current: Boolean
        @Composable get() =
            (LocalConfiguration.current.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES

    public actual fun current(): Boolean {
        val uiMode = Resources.getSystem().configuration.uiMode
        return (uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
    }
}
```

#### iOS

```kotlin
@OptIn(InternalComposeUiApi::class)
public actual object AppTheme {
    public actual val current: Boolean
        @Composable get() = LocalSystemTheme.current == SystemTheme.Dark

    public actual fun current(): Boolean {
        val style = UIScreen.mainScreen.traitCollection.userInterfaceStyle
        return style == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
}
```

The class-level `@OptIn(InternalComposeUiApi::class)` is mandatory — `LocalSystemTheme` / `SystemTheme` live in `androidx.compose.ui` under `InternalComposeUiApi`, which is **not** in the global opt-in list (only `ExperimentalComposeUiApi` is). Omitting the annotation fails compilation.

`true` = dark, `false` = light.

#### Usage

```kotlin
// inside RootComponent.Render()
val systemDarkTheme = AppTheme.current
AppTheme(darkTheme = systemDarkTheme, localeTag = ...) {
    // ...
}
```

`AppTheme.current` follows the system. If the product offers a manual theme override (Light/Dark/System), the override is read from `:data-services:datastore` and passed instead.

### `:toolkit:localization` — `AppLocale.current`

```kotlin
// commonMain
public expect object AppLocale {
    public val current: String
        @Composable get

    public fun current(): String
}
```

Returns a BCP-47 language tag: `"en"`, `"en-US"`, `"de-DE"`, `"fr-FR"`.

#### Android

```kotlin
public actual object AppLocale {
    public actual val current: String
        @Composable get() {
            val appTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (appTags.isNotBlank()) return appTags
            val conf = LocalConfiguration.current
            val confTags = conf.locales.toLanguageTags()
            if (confTags.isNotBlank()) return confTags
            return Locale.getDefault().toLanguageTag()
        }

    public actual fun current(): String = Locale.getDefault().toLanguageTag()
}
```

The Composable accessor prefers `AppCompatDelegate.getApplicationLocales()` — AppCompat exposes the user's per-app language preference uniformly across API levels (it backports the storage on pre-Android 13 and reads the platform per-app locale on Android 13+). Falls back to the current `LocalConfiguration` locales, then `Locale.getDefault()`.

#### iOS

```kotlin
public actual object AppLocale {
    public actual val current: String
        @Composable get() = normalizeTag(rememberSystemTag())

    public actual fun current(): String = systemTagNow()
}

@Composable
private fun rememberSystemTag(): String {
    var tag by remember { mutableStateOf(systemTagNow()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observers = mutableListOf<Any>()
        fun observe(name: String) {
            observers += center.addObserverForName(name, `object` = null, queue = null) { _ ->
                tag = systemTagNow()
            }
        }
        UIApplicationWillEnterForegroundNotification?.let { observe(it) }
        UIApplicationDidBecomeActiveNotification?.let { observe(it) }
        onDispose { observers.forEach { center.removeObserver(it) } }
    }
    return tag
}

private fun systemTagNow(): String =
    (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"

private fun normalizeTag(tag: String): String = tag.replace('_', '-')
```

`NSLocale.preferredLanguages` already returns BCP-47-shaped tags (`"de-DE"`); `normalizeTag` is a defensive `_`→`-` swap. The `DisposableEffect` re-reads the tag when the app returns to the foreground / becomes active, so a language switch in Settings flows through without an app relaunch.

#### Usage

```kotlin
// inside RootComponent.Render()
val systemLocaleTag = AppLocale.current

LaunchedEffect(systemLocaleTag) {
    DateFormatting.install(systemLocaleTag)
}

AppTheme(darkTheme = ..., localeTag = systemLocaleTag) {
    // ...
}
```

```kotlin
// inside BackendClient.defaultRequest (non-Composable)
header(HttpHeaders.AcceptLanguage, AppLocale.current())
```

### Two accessors — why

- **`val current: @Composable Boolean/String`**: lifecycle-aware, recomposes when the value changes (system theme switch, locale switch).
- **`fun current(): Boolean/String`**: synchronous; for non-Composable contexts (`BackendClient`, `RootViewModel`'s init).

Both must agree on the current value. If they diverge (e.g. one reads `AppCompatDelegate`, the other reads `Locale.getDefault()`), there's a bug — choose the priority order and commit.

### Why these are toolkit modules

`AppTheme` and `AppLocale` are read by:

- `:design-system:core/AppTheme` Composable wrapper (`darkTheme` parameter).
- `:toolkit:date-utils/DateFormatting.install(localeTag)`.
- `:data-services:backend/BackendClient.defaultRequest` (`Accept-Language`).

Different layers consume them. Toolkit is the lowest layer where the dependency can live without breaking the data layer's "no UI" rule.

### Build

```kotlin
// :toolkit:theme
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.theme" }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
    }
}

// :toolkit:localization — same plugins; needs androidx.appcompat on Android
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.localization" }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.appcompat)   // AppCompatDelegate.getApplicationLocales()
    }
}
```

### Rules

- **`AppTheme.current` follows system unless overridden by user preference**. The override layer is `:data-features:local-settings` (read from `:data-services:datastore`).
- **`AppLocale.current` follows system unless overridden**.
- **The `Composable` accessors must be wrapped in a `LaunchedEffect` for downstream side effects** (e.g. `DateFormatting.install(localeTag)`).

### Anti-patterns

- **Caching `AppTheme.current` in a `var` field.** It's a CompositionLocal read; cache at the call site if needed.
- **Bypassing `AppLocale.current` and using `Locale.getDefault()` directly.** Locale fallback differs (the project's accessor checks `AppCompatDelegate` first).
- **Hardcoded `darkTheme = false`** in `AppTheme(...)` outside debug. Always read from the system or user preference.

## :toolkit:image-loader

### Module shape

```kotlin
@Module(includes = [HttpModule::class])
@ComponentScan
public class ImageLoaderModule {

    @Single
    internal fun imageLoaderFactory(
        httpClient: HttpClient,
    ): SingletonImageLoader.Factory = SingletonImageLoader.Factory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .maxBitmapSize(Size(400, 400))
            .components { add(KtorNetworkFetcherFactory(httpClient)) }
            .build()
    }

    @Single(createdAtStart = true)
    internal fun imageLoaderInitializer(
        factory: SingletonImageLoader.Factory,
    ): ImageLoaderInitializer = ImageLoaderInitializer(factory)
}

// internal/ImageLoaderInitializer.kt
internal class ImageLoaderInitializer(factory: SingletonImageLoader.Factory) {
    init { SingletonImageLoader.setSafe(factory) }
}
```

- `@Module(includes = [HttpModule::class])` pulls in the shared `HttpClient`.
- The provider produces a `SingletonImageLoader.Factory` — Coil 3's hook for lazy, platform-aware `ImageLoader` construction. The factory's lambda receives a `PlatformContext` from Coil itself, so no `NativeContext`/`expect/actual` plumbing is needed in this module.
- `.maxBitmapSize(Size(400, 400))` caps decoded bitmaps; thumbnails are rendered at avatar/card sizes anyway.
- `ImageLoaderInitializer` is `@Single(createdAtStart = true)` — Koin instantiates it eagerly during `startKoin`, which calls `SingletonImageLoader.setSafe(factory)` before any Composable runs.

### Installation

Installation is entirely DI-driven on both platforms — there is no manual `SingletonImageLoader.setSafe { ... }` call in the app shells. As long as `ImageLoaderModule` is in `Koin.init { ... modules(...) }` (see `:shared/Koin.kt`), the `createdAtStart = true` initializer runs once, idempotently, on startup.

### Why Ktor adapter

Coil 3 ships with `coil-network-okhttp` (Android-only), `coil-network-ktor3` (multiplatform), and others. The Ktor adapter:

- **Reuses the base `HttpClient`** — platform engine and response validation stay consistent with the rest of the app.
- **Multiplatform** — works on both Android and iOS.
- **No second engine** — Ktor's engine handles both API calls and image fetches.

If image requests must carry backend auth or locale headers, pass a `HttpClient` configured with those plugins to the Coil builder. The default shape intentionally uses the base client; most product CDNs are public and should not receive bearer tokens.

### Caching

Coil 3 handles disk + memory cache automatically. Disk cache lives in the platform's cache directory; size cap is the default (~250 MB). Tune via `.diskCache { ... }` if needed.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.image.loader" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.httpClient)

        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}
```

No `compose.multiplatform.convention` and no `:toolkit:context` dep — Coil 3's `SingletonImageLoader.Factory` handles platform context internally, and `AsyncImage` is consumed by feature modules (which already have the Compose plugin), not by this module.

### Rules

- **One `ImageLoader` app-wide.** Singleton via Koin + `SingletonImageLoader.setSafe`.
- **Compose Composables use `AsyncImage` / `SubcomposeAsyncImage` from `coil-compose`.** Don't import Coil's lower-level types in feature code.
- **Placeholders/errors** come from `AppTokens.icons.<Name>` (vector, via `rememberInsetVectorPainter`) or `AppTokens.drawables.res(...)` (photographic), in `:design-system:resources:provider`.
- **`crossfade(true)`** for natural transitions; can be turned off per request.
- **`@Composable` callers don't read from network directly.** Pass URLs to `AsyncImage`.

### Anti-patterns

- **`Glide`, `Picasso`, `Kamel`** — different libraries, fragmented behavior. Coil 3 only.
- **Multiple `ImageLoader` instances** with different configs. Centralize.
- **Loading bitmaps manually via `BitmapFactory.decodeStream(...)`** — bypasses caching, leaks.
- **Hardcoded `MemoryCache.maxSizeBytes`** in feature code — set centrally if at all.
- **Network image inside a `LazyColumn` without `placeholder`** — first frame is empty; jarring.
