# Platform service structure — 2026-10-11

## Final boundaries

All three services use the same boundary: `api` contains consumer/bootstrap/transport contracts; `core` exposes only its annotated DI module; `core/internal` contains Kotlin-internal engine implementations, platform locks and supporting state/schema/configuration types. Tests mirror the internal package using the standard friend-module access.

AnalyticsModule registers its own internal AnalyticsConfiguration via @Single and a platform expect/actual factory. Android reads Application flags from the module's own Koin scope; iOS reads the native build mode. Removed ApplicationConfigurationModule, its shared registration and the now-unneeded shared iOS test setup. Platform configuration tests now belong to analytics/core.

## Naming and structure

- Engines: DefaultAnalytics, DefaultCrashReporting, DefaultMessaging; public composition modules: AnalyticsModule, CrashReportingModule, MessagingModule.
- Locks: AnalyticsLock, CrashReportingLock, MessagingLock, each in a dedicated expect declaration and matching actual files under internal.
- Diagnostics: AnalyticsDisposition, AnalyticsDiagnostic, AnalyticsValidationMode, CrashReportingDisposition each have a named file; all are internal.
- Provider names reflect the returned service: provideAnalyticsConfiguration, provideAnalyticsCatalog, provideAnalytics, provideCrashReporting, provideMessaging.
- DeferredPushTokenSynchronizer lives in messaging/core/internal. It keeps the original callback-time, container-local resolution needed to break the authorization → messaging cycle.
- Public API names were checked against their consumers. PushTokenParser is an actual nonthrowing SDK/Swift boundary contract and stays public. Private API validation helpers stay file-private.

## Review loop

1. Inspect external usage before changing visibility: concrete engines/configuration were used only by shared tests, not production consumers.
2. Move production implementations, make visibility internal, split helper declarations, update expect/actual packages and generated DI providers.
3. Relocate DI tests to the owning cores rather than exposing internals to shared. Messaging isolation/cycle tests are common, so both Android and native iOS exercise them. CrashReporting alias test also runs on both.
4. Check generated KSP bindings: interfaces and internal concrete type refer to one single definition; analytics configuration and catalog resolve without an application-wide configuration module.
5. Re-review source: no engine queue/cancellation/consent/callback behavior changes; no workers, locks, queues or SDK calls added; deferred handler still allocates once per engine, just like the prior anonymous object. No new generic service abstraction or runtime.

## Verification

- `/tmp/grippo-services-internal-tests.log`: analytics/crash-reporting/messaging core Android and native iOS tests plus shared Android tests — BUILD SUCCESSFUL in 39s.
- `/tmp/grippo-services-internal-build.log`: analytics API Android/native tests; Messaging common DI tests on Android/native; Android Debug/Release builds; shared Debug XCFramework for device/simulator; Android Debug/Release lint — BUILD SUCCESSFUL in 6m 7s.
- Test reports: Android analytics API 2 + analytics core 28 + crash core 11 + messaging core 22 + shared 24 = 87. Native iOS analytics API 2 + analytics core 25 + crash core 11 + messaging core 21 = 59. Zero failures/errors/skips.
- Lint: zero errors/fatal issues, 14 existing warnings per variant.
- `/tmp/grippo-services-internal-swift-simulator.log`: Swift Debug simulator — BUILD SUCCEEDED.
- `/tmp/grippo-services-internal-swift-device.log`: Swift Debug device unsigned — BUILD SUCCEEDED.
- Debug APK descriptor scan: only AnalyticsModule, CrashReportingModule and MessagingModule remain directly in their core root packages. All engine implementations are under core/internal. No removed ApplicationConfigurationModule or old root engine descriptors.
- Debug XCFramework Swift header contains no DefaultAnalytics, DefaultCrashReporting, DefaultMessaging, AnalyticsConfiguration or ApplicationConfigurationModule.
- Generated KSP module was inspected: analytics configuration/catalog are registered by AnalyticsModule; consumer and bootstrap bindings share each engine.
- Cross-module source scan finds no production imports of concrete engines/configuration and no removed ApplicationConfigurationModule references. `git diff --check` passed.
- No Firebase sends, app launches, token fetches, test crashes, mapping/symbol uploads or new feature integrations. Native app compilation covered Debug device/simulator; native core tests explicitly verify debug/release analytics policy via injected configurations.
