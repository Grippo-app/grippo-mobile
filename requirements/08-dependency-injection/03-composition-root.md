# Composition Root: `:shared/Koin.kt`

`:shared/Koin.kt` is **the single source of truth** for which Koin modules are active. Everything that participates in DI is listed here by name.

## File contents

```kotlin
public object Koin {
    public fun init(
        appDeclaration: KoinAppDeclaration = {},
    ): KoinApplication = KoinPlatformTools.defaultContext().startKoin {
        appDeclaration()
        modules(
            ContextModule().module,
            DatabaseModule().module,
            DataStoreModule().module,
            BackendModule().module,
            GoogleAuthModule().module,
            AppleAuthModule().module,
            CoreModule().module,
            DialogModule().module,
            AuthorizationFeatureModule().module,
            ErrorModule().module,
            UserFeatureModule().module,
            WeightHistoryFeatureModule().module,
            GoalFeatureModule().module,
            MusclesFeatureModule().module,
            EquipmentFeatureModule().module,
            ExerciseExamplesFeatureModule().module,
            ExcludedMusclesFeatureModule().module,
            TrainingsFeatureModule().module,
            ExcludedEquipmentFeatureModule().module,
            FeatureApiModule().module,
            ConnectivityModule().module,
            LinkOpenerModule().module,
            NotificationManagerModule().module,
            PermissionManagerModule().module,
            ResourcesProviderModule().module,
            SerializationModule().module,
            ExerciseMetricsFeatureModule().module,
            LocalSettingsFeatureModule().module,
            HttpModule().module,
            ImageLoaderModule().module,
        )
    }
}
```

## Calling `Koin.init`

### Android (`App.onCreate`)

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

- `androidContext(this@App)` registers the Android `Context` so `ContextModule` can provide `NativeContext`.
- `androidLogger()` enables Koin's Android log integration.
- `FirebaseProvider.setup(analytics, crashlytics, messaging)` injects the platform implementations into the static `FirebaseProvider` `object`. The provider is **not** a Koin singleton — it's a hand-wired static, so `setup` is a separate step after `Koin.init`.

### iOS (`iosApp/iOSApp.swift` + `iosApp/AppDelegate.swift`)

`Koin.init` runs in `iOSApp.init`. Firebase configuration is **not** in `iOSApp.init` — it must run from `AppDelegate.application(_:didFinishLaunchingWithOptions:)`, because at the moment `iOSApp.init` fires, the `AppDelegate` is not yet attached to `UIApplication` and Firebase's delegate swizzling fails.

```swift
@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate: AppDelegate

    init() {
        Koin().doInit(appDeclaration: { _ in })   // calls Kotlin's Koin.init {}
    }
    // ...
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        FirebaseApp.configure()                    // iOS Firebase SDK directly
        FirebaseProvider.shared.setup(
            analytics: IosFirebaseAnalytics(),
            crashlytics: IosFirebaseCrashlytics(),
            messaging: IosFirebaseMessaging()
        )
        // ...
        return true
    }
}
```

- `Koin().doInit(appDeclaration: { _ in })` is the Kotlin/Native interop of `Koin.init`. `Koin()` constructs the Kotlin `object` proxy; `doInit(appDeclaration:)` invokes the Kotlin function. The closure parameter is the `KoinAppDeclaration` lambda — empty on iOS.
- `FirebaseProvider.shared.setup(...)` is the Swift name for the static `FirebaseProvider.setup(...)` Kotlin function exposed via the `shared` XCFramework.

## Order of modules

**Order does not matter.** Koin resolves dependencies lazily — `inject<X>()` finds `X` regardless of which module declared it first.

But the **logical** grouping in this file mirrors the dependency direction:

1. Infrastructure first: `ContextModule`, `DatabaseModule`, `DataStoreModule`, `BackendModule`.
2. Platform auth: `GoogleAuthModule`, `AppleAuthModule`.
3. Core MVI: `CoreModule`, `DialogModule`, `ErrorModule`.
4. Features alphabetically (or by domain): `AuthorizationFeatureModule`, `UserFeatureModule`, ...
5. Toolkit: `ConnectivityModule`, `LinkOpenerModule`, `NotificationManagerModule`, ...
6. Resources and HTTP: `ResourcesProviderModule`, `SerializationModule`, `HttpModule`, `ImageLoaderModule`.

This ordering aids review — a reviewer can spot a misplaced new module ("a feature module sits between toolkit modules — wrong group").

## Adding a module — checklist

1. The module exists at `:<group>:<name>` and is in `settings.gradle.kts`.
2. `:shared/build.gradle.kts` has `implementation(projects.<group>.<name>)`.
3. **`:shared/Koin.kt`** has `<X>Module().module` added in `modules(...)`.

Missing step 3 → runtime "no definition found" error from any caller that `inject<...>()` something the module provides.

## Why explicit listing

Koin supports `module { ... }` discovery and even classpath scanning, but the project chooses **explicit** registration. Reasons:

- **Auditable.** A reviewer sees every active module in one place.
- **Reproducible.** No magic — what's listed is what runs.
- **Multiplatform-safe.** No reflection.
- **Cold-start cost minimized.** No classpath scanning at startup.

The cost: adding a module is a three-step process. Treat it as a deliberate API surface change.

## What `Koin.init` doesn't do

- **It doesn't start coroutines.** Subscriptions live in `RootViewModel.init`.
- **It doesn't set up Firebase.** `FirebaseProvider.setup(analytics, crashlytics, messaging)` is called separately on Android (`App.onCreate`, after `Koin.init`); iOS calls `FirebaseApp.configure()` and `FirebaseProvider.shared.setup(...)` from `AppDelegate.application(_:didFinishLaunchingWithOptions:)`. `FirebaseProvider` is a static `object`, not a Koin singleton.
- **It doesn't preload data.** First Repository call triggers the first DAO/HTTP read.
- **It doesn't decide the start screen.** `RootRouter.initialConfiguration = RootRouter.Home` (or wherever) does.

## Multiple Koin contexts

The project uses a **single** Koin context. Multi-context setups (e.g. one Koin per user session) add complexity without proportional benefit.

If a feature requires session-scoped state, use Koin's custom scopes (`@Scoped`, `Koin.createScope(...)`) within the single context.

## When tests want a different module

The project does not write tests by default. If/when a test needs to swap a dependency:

```kotlin
@BeforeTest
fun setUp() {
    startKoin {
        modules(
            ContextModule().module,
            // ... all real modules except the one being faked
            module { single<TrainingsFeature> { FakeTrainingsFeature() } },
        )
    }
}

@AfterTest
fun tearDown() {
    stopKoin()
}
```

Manual DSL is OK here — tests are short-lived and need overrides. The convention only forbids hand-DSL in production code.

## Anti-patterns

- **Forgetting to update `Koin.init` when adding a feature.** Runtime crash.
- **Letting Koin scan the classpath instead of explicit listing.** Magic + cold-start cost.
- **Multiple `Koin.init` calls.** Koin is global; the second call collides with the first. If you need to restart (e.g. switch environments at runtime), call `stopKoin()` first.
- **Registering modules from inside an Activity** instead of `App.onCreate`. The Activity is recreated on rotation; you'd register twice.
