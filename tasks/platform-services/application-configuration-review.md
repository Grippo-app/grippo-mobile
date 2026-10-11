# Application configuration module — 2026-10-11

## Change

The composition root registers `ApplicationConfigurationModule().module` alongside core modules. The configuration provider is `@Single` in an annotated `expect/actual` module, matching ContextModule. The module lives in `shared/configuration`, not the shared root. Removed the manual configuration DSL and `analyticsDebug` parameter from Kotlin/Swift startup.

Android resolves the real Application from its own Koin scope and reads FLAG_DEBUGGABLE. Missing Application is a wiring failure, without a default debug flag. iOS reads Platform.isDebugBinary of the linked Kotlin binary. Xcode configuration-specific XCFramework selection was checked: debug for Debug and release for Release.

## Self-review loop

- Correctness: startup registers Android Application before module resolution; existing singleton core aliases and sink bootstrap order remain intact.
- Architecture: build configuration belongs to the application composition root. Toolkit core remains platform-independent, with AnalyticsConfiguration injected by type.
- Simplicity: no constructor arguments unsupported by this KSP version, mutable globals, string properties, additional runtimes, or compatibility overloads.
- Performance: one lazy singleton configuration; native build flag and Android application flags read once. No workers, locks, or IO added.
- Tests: native generated module singleton and binary mode; Android explicit missing-Application failure; analytics generated module honors debug fail-fast and release rejection/disabled diagnostics.
- Follow-up review: application shells no longer duplicate the debug policy. Production Koin registrations use generated modules only. No Firebase runtime/send checks or uploads.

## Verification

Pending final Android/iOS builds, focused tests, lint, and Swift application compilation.
