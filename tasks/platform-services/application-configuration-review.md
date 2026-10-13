# Application configuration module — 2026-10-11

## Change

The composition root registers `ApplicationConfigurationModule().module` alongside core modules. The configuration provider is `@Single` in an annotated `expect/actual` module, matching ContextModule. The module lives in `shared/configuration`, not the shared root. Removed the manual configuration DSL and `analyticsDebug` parameter from Kotlin/Swift startup.

Android resolves the real Application from its own Koin scope and reads FLAG_DEBUGGABLE. Missing Application is a wiring failure, without a default debug flag. iOS reads Platform.isDebugBinary of the linked Kotlin binary. Xcode configuration-specific XCFramework selection was checked: debug for Debug and release for Release.

## Self-review loop

- Correctness: startup registers Android Application before module resolution; existing singleton core aliases and sink bootstrap order remain intact.
- Architecture: build configuration belongs to the application composition root. Toolkit core remains platform-independent, with AnalyticsConfiguration injected by type.
- Simplicity: no constructor arguments unsupported by this KSP version, mutable globals, string properties, additional runtimes, or compatibility overloads.
- Performance: one lazy singleton configuration; native build flag and Android application flags read once. No workers, locks, or IO added.
- Follow-up review: application shells no longer duplicate the debug policy. Production Koin registrations use generated modules only. No Firebase runtime/send checks or uploads.

## Verification

Completed checks:

- `/tmp/grippo-configuration-focused.log`: shared iOS compilation — BUILD SUCCESSFUL.
- `/tmp/grippo-configuration-build.log`: Android Debug/Release builds, Debug XCFramework, and both Release native frameworks completed. Release XCFramework packaging then failed on duplicate Compose resources.
- Recovery: the generated iosArm64 Release framework retained 14 old embedded Compose files. All 14 were byte-identical to current aggregated resources. Moved only that generated copy to `/tmp/grippo-configuration-stale-release-resources`; no source-resource changes and no EXCLUDE duplicate strategy. Packaged the already successfully compiled Release frameworks with their link tasks excluded. A repeated full linking invocation was interrupted because only import formatting had changed and the packaging failure did not require repeated native optimization.
- `/tmp/grippo-configuration-final-recovery.log`: Release XCFramework packaging, current shared iOS compilation, Android Debug/Release lint — BUILD SUCCESSFUL in 2m 5s.
- Lint: zero errors/fatal issues; 14 existing warnings per variant.
- Swift simulator Debug: `/tmp/grippo-configuration-swift-debug.log` — BUILD SUCCEEDED.
- Swift device Debug unsigned: `/tmp/grippo-configuration-swift-device.log` — BUILD SUCCEEDED.
- Swift simulator Release: `/tmp/grippo-configuration-swift-release.log` — BUILD SUCCEEDED.
- Merged Android manifests: Debug debuggable=true; Release false by default. Xcode selects debug/release XCFramework according to configuration. Generated KSP module registers AnalyticsConfiguration as single.
- `git diff --check` passed. No Firebase sends, application launches, token retrievals, synthetic crashes, mapping/symbol uploads, dependency upgrades, or feature integrations.
