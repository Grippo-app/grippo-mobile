# Shared composition and platform startup

Source of truth: `shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt` and
`shared/src/commonMain/kotlin/com/grippo/shared/bootstrap/ApplicationBootstrap.kt`. Inspect them
before relying on a detail below.

## Current shape (verified)

- `Koin.init(appDeclaration: KoinAppDeclaration = {}): ApplicationBootstrap` runs
  `KoinPlatformTools.defaultContext().startKoin { appDeclaration(); modules(...) }`.
- The `modules(...)` call lists 63 generated modules explicitly as `<Name>Module().module`,
  grouped in this order: `AnalyticsModule`; the 30 `<Feature>AnalyticsModule`s;
  `CrashReportingModule`, `MessagingModule`, `ContextModule`; `DatabaseModule`,
  `DataStoreModule`, `BackendModule`, `GoogleAuthModule`, `AppleAuthModule`; `CoreModule`,
  `DialogModule`; the `<Feature>FeatureModule`s (with `ErrorModule` among them);
  `FeatureApiModule().module`; the remaining toolkit modules (connectivity, link opener,
  notifications, permissions, resources provider, serialization, http client, image loader).
- Resolution starts only after `startKoin` returns, so list position does not order
  resolution. Keep the file's grouping when adding a module.
- `init` then resolves `analytics`, `crashReporting`, `messaging`, `LocalSettingsFeature` and the
  `Analytics` collection controller from the started context and returns `ApplicationBootstrap(...)`.
- 29 of the 30 `:analytics:<feature>` modules register
  `@Single @Named("<feature>_analytics") internal fun provideSchema(): AnalyticsSchema`;
  `DebugAnalyticsModule` registers none. `AnalyticsModule.provideAnalyticsCatalog` collects them
  with `scope.getAll<AnalyticsSchema>()` the first time the analytics core is resolved.
- `ApplicationBootstrap` is a class with an `internal constructor` exposing
  `analytics: AnalyticsBootstrap`, `crashReporting: CrashReportingBootstrap` and
  `messaging: MessagingBootstrap`. Its `init` observes `LocalSettingsFeature.observeAnalyticsConsent()`
  (`distinctUntilChanged`, `retryWhen` with a growing delay that reports `Denied` meanwhile) and
  calls `updateCollection(Granted|Denied)` on the analytics core for the application lifetime.
- Android: `androidApp/src/main/java/com/grippo/android/App.kt` calls
  `Koin.init { androidContext(this@App); androidLogger() }`, then
  `bootstrap.analytics.attachSink(...)`, `bootstrap.crashReporting.attachSink(...)` and
  `bootstrap.messaging.attachTokenProvider(...)` with the `data-services/firebase` adapters.
- iOS: `iosApp/iosApp/AppDeletage.swift` (the file name typo is real; the class is `AppDelegate`)
  calls `Koin().doInit(appDeclaration:)` once, keeps the bootstrap and attaches the Swift adapters
  through the same three contracts.
- Cross-layer binding to know about: the `toolkit/messaging` API contract `PushTokenSynchronizer`
  is implemented by `PushTokenSynchronizerImpl` (`@Single(binds = [PushTokenSynchronizer::class])`)
  in `data-features/authorization`; the messaging core resolves it lazily through that binding.

## Rules

- Register a new module by its generated `.module` in `Koin.init` and add the matching
  `implementation(projects...)` dependency to `shared/build.gradle.kts`
  ([new module checklist](../../platform-build-toolkit/references/new-module.md)). Preserve the
  existing composition unless the authorized scope covers a startup change.
- Analytics implementations depend on API contracts, not UI/domain/SDK transport; a new feature
  analytics module registers its qualified schema provider like its siblings
  ([feature-analytics](../../feature-analytics/SKILL.md)).
- Android transport adapters are app-owned in `data-services/firebase/`; only `:androidApp` depends
  on that module. Swift SDK adapters live in `iosApp`. Shared code and features use toolkit contracts.
- Shells call `Koin.init` once and attach adapters through `ApplicationBootstrap`; they define no
  feature DI. DI wiring MUST NOT introduce feature-owned lifecycle binding in routes or render.
