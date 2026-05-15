---
productName: Grippo
productPackage: com.grippo
apiClassName: GrippoApi
backendHost: grippo-app.com
applicationId: com.grippo.android
iosFrameworkName: shared
iosEnabled: true
firebaseEnabled: true
codexEnabled: auto
prelaunch: false
supportedLocales:
  - en
  - uk
  - ru
typefaceFactory: manrope
featuresWithRootComponentSuffix:
  - home
  - trainings
diHandWrittenModules:
  - GoogleAuthModule
  - AppleAuthModule
---

# Project config — single source of truth

Sub-agents under `requirements/sub-agents/` read this file before acting. When you bootstrap a new project from these requirements, copy this file and update every value.

## Field meanings

- `productName` — used in class prefixes (`<Product>Api`, `<Product>Component`).
- `productPackage` — Kotlin package root.
- `apiClassName` — name of the flat backend API class.
- `backendHost` — used in `BackendClient.defaultRequest`.
- `applicationId` — Android Play Store id.
- `iosFrameworkName` — XCFramework target name.
- `iosEnabled` — if false, build-validator skips iOS gates.
- `firebaseEnabled` — if false, agents skip Firebase wiring.
- `codexEnabled` — controls the external-review gate. One of:
  - `auto` (default) — orchestrator detects the [openai/codex-plugin-cc](https://github.com/openai/codex-plugin-cc) plugin at runtime. If present, runs `codex-review-loop`; otherwise falls back to `internal-reviewer` (Claude-backed local review).
  - `true` — force `codex-review-loop`; orchestrator hard-fails if the plugin is missing.
  - `false` — force `internal-reviewer`; skip Codex detection entirely.
- `prelaunch` — if true, room-migration-builder allows destructive fallback without a migration.
- `supportedLocales` — resource-builder requires every locale to receive each new key.
- `typefaceFactory` — resource-builder uses this factory function name when registering fonts.
- `featuresWithRootComponentSuffix` — features that use `*RootComponent.kt` instead of bare `*Component.kt` (because they have a sub-screen with the same name as the feature).
- `diHandWrittenModules` — Koin modules that legitimately use the hand-written `module { … }` DSL outside annotated `@Single` classes (platform-edge wrappers, etc.). `di-validator` reads this list before flagging hand-DSL hits.

## Updating

When a value changes (new locale added, app ships and `prelaunch` flips, codex installed), update this file. Sub-agents read it lazily — no rebuild needed.
