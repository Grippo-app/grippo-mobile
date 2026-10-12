# Composition root — `:shared/Koin.kt`

`:shared/Koin.kt` is **the single source of truth** for which Koin modules are
active. Everything that participates in DI is listed here by name.

## Current composition and startup

Use `shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt` as the actual module
list. `Koin.init` starts the single Koin context and returns `ApplicationBootstrap`
with toolkit analytics/crashReporting/messaging startup contracts. Register
annotated modules explicitly through `.module`, including service cores,
feature analytics implementations and UI provider/classifier bindings. Components
resolve their stateless `<Feature>AnalyticsProvider`; it injects the feature API and
returns a scenario retained by that component context. Mutable scenarios are never
app-scoped DI singletons.

Feature telemetry follows [feature-analytics](../../feature-analytics/SKILL.md).
UI depends on `:analytics:analytics-api`; shared depends on `:analytics:<feature>`.
Custom schemas use qualified `@Single` declarations and must be registered before
Analytics is resolved. Do not add runtime module constructor parameters or
manual production `module { single { ... } }` blocks.

### Android (`App.onCreate`)

```kotlin
val bootstrap = Koin.init {
    androidContext(this@App)
    androidLogger()
}
val analytics = AndroidFirebaseAnalytics(FirebaseAnalytics.getInstance(this))
analytics.setCollection(AnalyticsCollection.Unknown)
bootstrap.analytics.attachSink(analytics)
bootstrap.crashReporting.attachSink(AndroidFirebaseCrashlytics(FirebaseCrashlytics.getInstance()))
bootstrap.messaging.attachTokenProvider(AndroidFirebaseMessaging())
```

Only Android app depends on the Firebase transport module. Adapters implement
toolkit contracts; features never call the SDK directly. Attaching an adapter
does not authorize collection or runtime validation sends.

### iOS (`AppDelegate`)

AppDelegate initializes Koin once in its initializer and retains the returned
ApplicationBootstrap. iOSApp hosts the root without a second Koin init.
Configure Firebase from `application(_:didFinishLaunchingWithOptions:)`, after
the delegate is attached, then attach Swift SDK adapters:

```swift
FirebaseApp.configure()
let analytics = IosFirebaseAnalytics()
analytics.setCollection(collection: .unknown)
bootstrap.analytics.attachSink(analyticsSink: analytics)
bootstrap.crashReporting.attachSink(crashSink: IosFirebaseCrashlytics())
bootstrap.messaging.attachTokenProvider(tokenProvider: IosFirebaseMessaging())
```

Current implementations: `iosApp/iosApp/AppDeletage.swift` and Android
`androidApp/src/main/java/com/grippo/android/App.kt` (paths relative to repo root).
Do not recreate a global Firebase provider or export Android transport to Swift.

## Order of modules

**Order does not matter.** Koin resolves dependencies lazily — `inject<X>()` finds
`X` regardless of which module declared it first.

The logical grouping below is a **recommendation**, not a hard rule. The reference
repo itself interleaves a couple of feature modules into other blocks (e.g.
`ExerciseMetricsFeatureModule` / `LocalSettingsFeatureModule` appear amid
resources/HTTP), and Koin works correctly either way. Use the grouping as a review
aid for new modules; don't reorder the existing list just to match.

1. Infrastructure first: `ContextModule`, `DatabaseModule`, `DataStoreModule`,
   `BackendModule`.
2. Platform auth: `GoogleAuthModule`, `AppleAuthModule`.
3. Core MVI: `CoreModule`, `DialogModule`, `ErrorModule`.
4. Features alphabetically (or by domain): `AuthorizationFeatureModule`,
   `UserFeatureModule`, ...
5. Toolkit: `ConnectivityModule`, `LinkOpenerModule`,
   `NotificationManagerModule`, ...
6. Resources and HTTP: `ResourcesProviderModule`, `SerializationModule`,
   `HttpModule`, `ImageLoaderModule`.

A new module placed in the "wrong" block still works; the reviewer can flag it but
it's a doc-only nit, not a correctness issue.

## Adding a module — checklist

1. The module exists at `:<group>:<name>` and is in `settings.gradle.kts`.
2. `:shared/build.gradle.kts` has `implementation(projects.<group>.<name>)`.
3. **`:shared/Koin.kt`** has `<X>Module().module` added in `modules(...)`.

Missing step 3 → runtime "no definition found" error from any caller that
`inject<...>()` something the module provides. Missing step 2 → compile failure
(`:shared` can't `import com.<org>.<product>.<feature>.<X>FeatureModule`).

### Worked recipe — Step 8: wire up in `:shared`

`:shared/build.gradle.kts`:

```kotlin
sourceSets.commonMain.dependencies {
    // ...
    implementation(projects.dataFeatures.notifications)
}
```

`:shared/Koin.kt`:

```kotlin
modules(
    // ... existing
    NotificationsFeatureModule().module,
)
```

## Why explicit listing

Koin supports `module { ... }` discovery and even classpath scanning, but the
project chooses **explicit** registration. Reasons:

- **Auditable.** A reviewer sees every active module in one place.
- **Reproducible.** No magic — what's listed is what runs.
- **Multiplatform-safe.** No reflection.
- **Cold-start cost minimized.** No classpath scanning at startup.

The cost: adding a module is a three-step process. Treat it as a deliberate API
surface change.

`:shared` is the composition root. To wire a Koin module into `:shared/Koin.kt`,
the module must be **reachable** as a dependency — the wholesale listing of every
module in `:shared/build.gradle.kts` is **intentional**. A single-implementation
feature module missing from that list means `Koin.init` won't see it.

## Startup responsibilities

Koin resolves app-lifetime toolkit cores, which can own bounded workers. It does
not configure Firebase, grant collection consent, preload feature data or decide
navigation. App shells attach platform adapters after SDK initialization; the
feature remains responsible for its business work and routes.

## Multiple Koin contexts

The project uses a **single** Koin context. Multi-context setups (e.g. one Koin
per user session) add complexity without proportional benefit.

If a feature requires session-scoped state, use Koin's custom scopes (`@Scoped`,
`Koin.createScope(...)`) within the single context.

## When tests want a different module

DI changes carry mandatory graph evidence (machine authority:
`orchestrator/tasks/test-policy.json` — `di-composition-root`): the
generated graph must compile, Koin host `verify()` covers the module list
(JVM/host evidence only, never Native), and a composition-root smoke test
boots the exact module set in an isolated context. Prefer a local
`koinApplication { ... }` per test; when a test must use the global context:

```kotlin
@BeforeTest
fun setUp() {
    startKoin {
        modules(
            ContextModule().module,
            // ... all real modules except the one being faked
            module { single<NoteFeature> { FakeNoteFeature() } },
        )
    }
}

@AfterTest
fun tearDown() {
    stopKoin()
}
```

Manual DSL is OK here — tests are short-lived and need overrides. The convention
only forbids hand-DSL in production code.

## Anti-patterns

- **Forgetting to update `Koin.init` when adding a feature.** Runtime crash.
- **Letting Koin scan the classpath instead of explicit listing.** Magic +
  cold-start cost.
- **Multiple `Koin.init` calls.** Koin is global; the second call collides with
  the first. If you need to restart (e.g. switch environments at runtime), call
  `stopKoin()` first.
- **Registering modules from inside an Activity** instead of `App.onCreate`. The
  Activity is recreated on rotation; you'd register twice.
