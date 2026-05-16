---
productName: <Product>
productPackage: com.<org>.<product>
apiClassName: <Product>Api
backendHost: <product-domain>.com
applicationId: com.<org>.<product>.android
iosFrameworkName: shared
iosEnabled: true
firebaseEnabled: true
codexEnabled: auto
prelaunch: true
supportedLocales:
  - en
typefaceFactory: <typeface>
featuresWithRootComponentSuffix: []
diHandWrittenModules: []
---

> **Fresh-project state.** Every value in the frontmatter above is a placeholder or a neutral default. Before invoking any sub-agent (`orchestrator`, builders, validators), replace every `<placeholder>` with project-specific values per `requirements/launch.md` Step 1.5. Empty arrays (`featuresWithRootComponentSuffix: []`, `diHandWrittenModules: []`) stay empty until the project actually needs them — sub-agents update them on demand.

# Project config — single source of truth

Replace every value in the frontmatter above before the first bootstrap. Placeholders use the same syntax as `00-overview/05-template-conventions.md` §1.

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
- `featuresWithRootComponentSuffix` — features that use `*RootComponent.kt` instead of bare `*Component.kt` (because they have a sub-screen with the same name as the feature). Start empty (`[]`) on a fresh project; the orchestrator appends a feature name only when its first sub-screen collides with the feature root.
- `diHandWrittenModules` — Koin modules that legitimately use the hand-written `module { … }` DSL outside annotated `@Single` classes (platform-edge wrappers, etc.). `di-validator` reads this list before flagging hand-DSL hits. Start empty (`[]`) on a fresh project; append a module name only when a hand-written `module { … }` block is deliberately introduced (typically platform-edge wrappers like Google/Apple auth).

## Updating

When a value changes (new locale added, app ships and `prelaunch` flips, codex installed), update this file. Sub-agents read it lazily — no rebuild needed.
