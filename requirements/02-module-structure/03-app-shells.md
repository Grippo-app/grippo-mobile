# App Shells: `:androidApp` and `:iosApp`

App shells are **thin**. They contain **no** business logic, **no** mappers, **no** screen code. They:

1. Initialize the DI container (`Koin.init { ... }`).
2. Start the Decompose `RootComponent`.
3. Wire platform-specific services (Firebase, splash screen, deeplinks).

Everything else — including the UI — comes from `:shared`.

## `:androidApp`

### Structure

```
androidApp/
  build.gradle.kts                          // android.application.convention + plugins
  google-services.json                      // Firebase config (gitignored variant per env)
  src/main/
    AndroidManifest.xml                     // single-activity, deeplinks, push intent filters
    java/com/<org>/<product>/android/
      App.kt                                // Application subclass; starts Koin + Firebase
      MainActivity.kt                       // single Activity hosting Decompose root
    res/                                    // Android-only resources (launcher icons, splash)
```

### `App.kt`

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Koin.init {
            androidContext(this@App)
            androidLogger()
        }
        FirebaseProvider.setup(
            analytics = AndroidFirebaseAnalytics(core = FirebaseAnalytics.getInstance(this)),
            crashlytics = AndroidFirebaseCrashlytics(core = FirebaseCrashlytics.getInstance()),
            messaging = AndroidFirebaseMessaging(),
        )
    }
}
```

`App` and `MainActivity` are not marked `public` or `internal` because `:androidApp` is a single-target Android module — `explicitApi()` is only enforced inside KMP modules.

### `MainActivity.kt` (essentials)

```kotlin
class MainActivity : ComponentActivity() {

    private val root: RootComponent by lazy {
        retainedComponent {
            RootComponent(
                componentContext = it,
                close = ::finishAffinity,
                deeplink = intent.getStringExtra(LocalNotificationExtras.DEEPLINK),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(/* status + nav bar styles */)

        setContent { root.Render() }
    }

    // Warm start: app already running, user tapped a notification
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(LocalNotificationExtras.DEEPLINK)
            ?.let { root.handleDeeplink(it) }
    }
}
```

Notes:
- `installSplashScreen()` from `androidx-core-splashscreen`.
- `enableEdgeToEdge()` — system bars are translucent; content draws underneath.
- `retainedComponent { ... }` — Decompose helper that survives configuration changes and returns the same component instance.
- `RootComponent.close` is invoked when the root navigator emits `RootDirection.Close`. On Android it is `::finishAffinity` (closes the task). On iOS the AppDelegate passes a closure that suspends the app.
- `deeplink` is a raw `String?` lifted out of the launch intent's extras. The `Deeplink` enum (in `:ui-screen-features:screen-api`) parses the key inside `RootViewModel.parseDeeplink(raw)`.
- `onCreate` handles **cold start** (deeplink from launch intent); `onNewIntent` handles **warm start** (`root.handleDeeplink(key)` routes the key either to `enqueueDeeplink` or `applyDeeplink` depending on whether the user is already on Home). The split is intentional.

### `build.gradle.kts`

```kotlin
plugins {
    id("android.application.convention")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.<org>.<product>.android.app"

    defaultConfig {
        applicationId = "com.<org>.<product>.android"
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] = "<your-oauth-id>"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val default = getDefaultProguardFile("proguard-android-optimize.txt")
            proguardFiles(default, "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.uiCore.foundation)
    implementation(projects.toolkit.dateUtils)
    implementation(projects.toolkit.theme)
    implementation(projects.toolkit.notificationManager)
    implementation(projects.designSystem.core)

    implementation(compose.foundation)
    implementation(compose.material3)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)

    // Firebase
    implementation(projects.dataServices.firebase)
    implementation(project.dependencies.platform(libs.android.firebase.bom))
    implementation(libs.android.firebase.analytics)
    implementation(libs.android.firebase.crashlytics)
    implementation(libs.android.firebase.messaging)
}
```

Notes:
- The convention is `android.application.convention` (not `android.library.convention`).
- The Compose plugins are applied directly here, **not** via a convention plugin, because `:androidApp` is not a KMP module — it is a single-target Android app.
- `:androidApp` directly depends on `:shared`, `:design-system:core`, `:ui-core:foundation`, and a handful of toolkit modules it touches (for splash, notifications, theme).
- Firebase is wired here (not in `:shared`) because Firebase SDKs are Android-only.

### Manifest essentials

- Single `MainActivity` with `android:exported="true"`, `android:launchMode="singleTop"`, the `MAIN` / `LAUNCHER` intent filter, and a custom theme (`Theme.<Product>.Splash`) for `installSplashScreen()`.
- The reference repo's manifest declares **no** `<uses-permission>` entries — `POST_NOTIFICATIONS` is requested at runtime via `:toolkit:permission-manager`. Add `WAKE_LOCK` / `RECEIVE_BOOT_COMPLETED` only if a future feature schedules notifications with WorkManager/AlarmManager that need them.
- Add deeplink intent filters here only if/when the product exposes external deeplinks; the cold-start path currently reads the deeplink key out of an in-process `Intent` extra (`LocalNotificationExtras.DEEPLINK`).

## `:iosApp`

`:iosApp` is **not** a Gradle module — it is an Xcode project that links the static `XCFramework` produced by `:shared`.

### Structure

```
iosApp/
  iosApp.xcodeproj/                         // Xcode project
  iosApp/
    iOSApp.swift                            // @main entry point
    ContentView.swift                       // wraps RootViewController from iosMain
    Info.plist
    GoogleService-Info.plist
    Assets.xcassets/                        // iOS-only assets (app icon, launch image)
```

### Swift entry point (essentials)

```swift
@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate: AppDelegate

    init() {
        Koin().doInit(appDeclaration: { _ in })   // Koin.init — from :shared
    }

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root, backDispatcher: appDelegate.backDispatcher)
                .ignoresSafeArea(edges: .all)
        }
    }
}
```

`AppDelegate` owns the `RootComponent` and a `BackDispatcher` (from Essenty), and calls `FirebaseApp.configure()` + `FirebaseProvider.shared.setup(...)` from `application(_:didFinishLaunchingWithOptions:)`. Constructing the root in `AppDelegate` (rather than in `iOSApp.init`) ensures Firebase's swizzling can attach the delegate before any Firebase call runs.

### `iosMain/RootViewController.kt`

```kotlin
public fun rootViewController(
    root: RootComponent,
    backDispatcher: BackDispatcher,
): UIViewController = ComposeUIViewController(
    configure = { parallelRendering = true }
) {
    PredictiveBackGestureOverlay(
        backDispatcher = backDispatcher,
        backIcon = { progress, _ -> PredictiveBackGestureIcon(Icons.Default.ChevronLeft, progress) },
        modifier = Modifier.fillMaxSize(),
        content = { root.Render() },
    )
}
```

The exported Kotlin function takes the already-constructed `RootComponent` from `AppDelegate`; Swift calls it via the generated `RootViewControllerKt.rootViewController(root:backDispatcher:)`.

### XCFramework refresh

After editing Kotlin code:

```
./gradlew :shared:assembleSharedDebugXCFramework
```

The framework lands at `shared/build/XCFrameworks/debug/shared.xcframework`; Xcode picks it up on next build.

For Release:

```
./gradlew :shared:assembleSharedReleaseXCFramework
```

Update the Xcode reference to `../shared/build/XCFrameworks/release/shared.xcframework`. See `12-gradle-build/07-ios-swiftpackage.md` for the convention plugin behind this.

## What the shells MUST NOT do

- Define Composables (except the entry-point wrapping).
- Hold state classes or business logic.
- Import data-feature modules directly. They import `:shared`, which imports them.
- Configure Koin modules. The only Koin call here is `Koin.init { androidContext(this); androidLogger() }` (Android) or `Koin().doInit(appDeclaration: { _ in })` (iOS — the generated Swift name is `Koin`, not `KoinKt`, because `Koin` is a Kotlin `object`).
- Format dates, render strings, or know about resources.
