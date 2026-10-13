# Platform service structure — 2026-10-11

## Final boundaries

All three services use the same boundary: `api` contains consumer/bootstrap/transport contracts; `core` exposes only its annotated DI module; `core/internal` contains Kotlin-internal engine implementations, platform locks and supporting state/schema/configuration types.

AnalyticsModule registers its own internal AnalyticsConfiguration via @Single and a platform expect/actual factory. Android reads Application flags from the module's own Koin scope; iOS reads the native build mode. Removed ApplicationConfigurationModule, its shared registration.

## Naming and structure

- Engines: DefaultAnalytics, DefaultCrashReporting, DefaultMessaging; public composition modules: AnalyticsModule, CrashReportingModule, MessagingModule.
- Locks: AnalyticsLock, CrashReportingLock, MessagingLock, each in a dedicated expect declaration and matching actual files under internal.
- Diagnostics: AnalyticsDisposition, AnalyticsDiagnostic, AnalyticsValidationMode, CrashReportingDisposition each have a named file; all are internal.
- Provider names reflect the returned service: provideAnalyticsConfiguration, provideAnalyticsCatalog, provideAnalytics, provideCrashReporting, provideMessaging.
- DeferredPushTokenSynchronizer lives in messaging/core/internal. It keeps the original callback-time, container-local resolution needed to break the authorization → messaging cycle.
- Public API names were checked against their consumers. PushTokenParser is an actual nonthrowing SDK/Swift boundary contract and stays public. Private API validation helpers stay file-private.

## Review loop

1. Inspect external usage before changing visibility: concrete engines/configuration had no production consumers.
2. Move production implementations, make visibility internal, split helper declarations, update expect/actual packages and generated DI providers.
3. Check generated KSP bindings: interfaces and internal concrete type refer to one single definition; analytics configuration and catalog resolve without an application-wide configuration module.
4. Re-review source: no engine queue/cancellation/consent/callback behavior changes; no workers, locks, queues or SDK calls added; deferred handler still allocates once per engine, just like the prior anonymous object. No new generic service abstraction or runtime.

## Verification

- `/tmp/grippo-services-internal-build.log`: Android Debug/Release builds; shared Debug XCFramework for device/simulator; Android Debug/Release lint — BUILD SUCCESSFUL in 6m 7s.
- Lint: zero errors/fatal issues, 14 existing warnings per variant.
- `/tmp/grippo-services-internal-swift-simulator.log`: Swift Debug simulator — BUILD SUCCEEDED.
- `/tmp/grippo-services-internal-swift-device.log`: Swift Debug device unsigned — BUILD SUCCEEDED.
- Debug APK descriptor scan: only AnalyticsModule, CrashReportingModule and MessagingModule remain directly in their core root packages. All engine implementations are under core/internal. No removed ApplicationConfigurationModule or old root engine descriptors.
- Debug XCFramework Swift header contains no DefaultAnalytics, DefaultCrashReporting, DefaultMessaging, AnalyticsConfiguration or ApplicationConfigurationModule.
- Generated KSP module was inspected: analytics configuration/catalog are registered by AnalyticsModule; consumer and bootstrap bindings share each engine.
- Cross-module source scan finds no production imports of concrete engines/configuration and no removed ApplicationConfigurationModule references. `git diff --check` passed.
- No Firebase sends, app launches, token fetches, synthetic crashes, mapping/symbol uploads or new feature integrations. Native app compilation covered Debug device/simulator. Analytics policy remains build-specific.
