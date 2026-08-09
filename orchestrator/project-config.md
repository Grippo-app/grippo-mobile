---
productName: <Product>
productPackage: com.<org>.<product>
apiClassName: <Product>Api
backendHost: <product-domain>
applicationId: com.<org>.<product>
iosFrameworkName: shared
iosEnabled: true
firebaseEnabled: true
codexEnabled: auto
verifyEnabled: auto
prelaunch: true
supportedLocales:
  - en
typefaceFactory: <typeface>
featuresWithRootComponentSuffix: []
diHandWrittenModules: []
figmaEnabled: false
figmaLibraryUrl: <figma-library-url>
screenshotPixelGate: strict
androidAssembleTask: :androidApp:assembleDebug
sharedFrameworkTask: :shared:assembleSharedDebugXCFramework
roborazziRecordTask: recordRoborazziAndroidHostTest
moduleCompileTask: compileAndroidMain
backendContractEnabled: auto
---

> **Fresh-project state.** Every value in the frontmatter above is a placeholder or a neutral default. Before invoking any skill (`task-orchestrator`, the builder skills, the validation gates), replace the required identity/build placeholders with project-specific values per `orchestrator/launch.md` Step 1.5. The optional Figma placeholder may remain until that integration is enabled. Fresh projects start without `orchestrator/api-contract/environments.json`; Integrations → Backend creates the sole canonical Backend source manifest when the first source is added. Empty arrays (`featuresWithRootComponentSuffix: []`, `diHandWrittenModules: []`) stay empty until the project actually needs them — the orchestrator appends the former on demand; you append the latter by hand when a module deliberately uses a hand-written Koin `module { … }` block.

# Project config — core scalar source of truth

Replace every required identity/build value in the frontmatter above before the first bootstrap. Domain manifests may own structured integration settings without duplicating them here: `orchestrator/api-contract/environments.json` is the canonical Backend source manifest whenever it exists.

The skills under `.claude/skills/` read this config before acting. When you bootstrap a new project from these skills, copy this file and update the required values.

## Field meanings

- `productName` — used in class prefixes (`<Product>Api`, `<Product>Component`).
- `productPackage` — Kotlin package root.
- `apiClassName` — name of the flat backend API class.
- `backendHost` — used in `BackendClient.defaultRequest`.
- `applicationId` — Android Play Store id; also used as the iOS bundle id (no platform suffix).
- `iosFrameworkName` — XCFramework target name.
- `iosEnabled` — if false, build-validator skips iOS gates.
- `firebaseEnabled` — if false, agents skip Firebase wiring.
- `codexEnabled` — machine backing field for the Reviewer mode. Manage it through
  the Site's Reviewer control: **Automatic** selects Codex only when the shared
  local readiness detector confirms it is available, **Require Codex** blocks
  review when it is unavailable, and **Internal review only** always selects
  the internal reviewer. Once an attempt starts, the selected reviewer is
  locked; invocation failure never silently switches reviewers.
- `verifyEnabled` — controls the post-validator runtime-verify gate (orchestrator Step 4.6 — runs after validators are green, before external review). One of:
  - `auto` (default) — orchestrator invokes the Anthropic `verify` skill if the `Skill` tool is available in its runtime; if not, emits a manual-verify hint in the summary and records the gate as `deferred`. No hard fail.
  - `true` — force runtime verify; orchestrator hard-fails when the `Skill` tool is unavailable (use this when you want CI-like discipline).
  - `false` — skip the runtime-verify gate entirely. Useful for headless / CI environments where the app cannot be launched.
- `prelaunch` — if true, room-migration-builder allows destructive fallback without a migration.
- `supportedLocales` — resource-builder requires every locale to receive each new key.
- `typefaceFactory` — resource-builder uses this factory function name when registering fonts.
- `featuresWithRootComponentSuffix` — features that use `*RootComponent.kt` instead of bare `*Component.kt` (because they have a sub-screen with the same name as the feature). Start empty (`[]`) on a fresh project; the orchestrator appends a feature name only when its first sub-screen collides with the feature root.
- `diHandWrittenModules` — Koin modules that legitimately use the hand-written `module { … }` DSL outside annotated `@Single` classes (platform-edge wrappers, etc.). `di-validator` reads this list before flagging hand-DSL hits. Start empty (`[]`) on a fresh project; append a module name only when a hand-written `module { … }` block is deliberately introduced (typically platform-edge wrappers like Google/Apple auth).
- `figmaEnabled` — opt-in gate for the Figma → MCP → Compose tooling (the `orchestrator/figma/` sidecar + the Figma-aware skill flow). Default `false`: `launch.md` Step 6.5 is skipped, the Figma validators no-op, and the project is byte-for-byte a non-Figma project. When `true`, beyond the validators it also activates the UI-task design discipline: the `## Design`-section rule on screen/dialog tasks, the per-task screen cache (`figma:screens:<stem>` session, gated by the orchestrator's Step 1b pre-flight), the component census + design-system-first split (`task-prep` Step 5.5), and the `figma-spec-validator` gate. Set `true` only when the project has a Figma design library to bind. See the `implement-figma` skill.
- `figmaLibraryUrl` — the project's Figma design-library URL (the file/library the MCP binds to and the token/component pipelines read). Per-project; leave the `<figma-library-url>` placeholder until the project actually binds a library. The MCP is OAuth-bound per project — no access secret lives in config (there is no REST/token fallback).
- `screenshotPixelGate` — routes the screenshot gate's **pixel-similarity** verdict (only relevant when `figmaEnabled: true`; harmless otherwise). One of:
  - `strict` (default) — a pixel-similarity divergence (SSIM band / per-zone floor / colour) may **BLOCK**. Every bootstrap starts strict so pixel drift fails closed from day one.
  - `advisory` — the pixel comparison is computed and the 3-frame Figma/overlay/app evidence is always shown, but a similarity divergence is a **WARN**, never a hard block. The design-agnostic **structural** gate (`figma-spec-validator`) and the completeness/anti-forgery net (missing/stale/tampered captures, coverage) remain the strict signals, unaffected by this knob. Downgrade to this per-project only when a single global pixel threshold demonstrably over-blocks the design language.
  - `off` — computed and shown only; similarity findings are suppressed entirely. The comparison always runs at full canonical metric strictness regardless of the mode (only the verdict is routed); the orchestrator passes the value to `compare-screenshots.mjs` via `SCREENSHOT_PIXEL_GATE`.
- `designLocale` — OPTIONAL: the DESIGN language (one of `supportedLocales`, e.g. `designLocale: uk`) — the locale the Figma frames are written in, which the screenshot capture must render (`check-capture-config` derives the `@Config` locale segment from it; the comparator cross-checks the capture manifest's `localeTag` against it). Absent = auto-detected deterministically from the pulled spec texts × the app's string resources (`lib/design-locale.mjs`); add the key when detection reports `CAPTURE_LOCALE_UNDERIVABLE` (votable design text with no decisive locale match) or to pin the language explicitly. A value outside `supportedLocales` is a config error (fail-closed), never a silent skip.
- `androidAssembleTask` / `sharedFrameworkTask` / `roborazziRecordTask` / `moduleCompileTask` — the project's CANONICAL Gradle task names, read by the Figma tooling instead of hardcoding a module layout. `androidAssembleTask`/`sharedFrameworkTask` are FULL task paths (the standard build-gate acceptance bullets embed them; the framework gate applies only when `iosEnabled: true`); `roborazziRecordTask` is the per-module record-task SUFFIX (`run-figma-gates.mjs --stage screenshot` invokes `<module>:<suffix>`); `moduleCompileTask` is the per-module KMP compile SUFFIX (`compileAndroidMain` for `com.android.kotlin.multiplatform.library` modules). Absent keys use the documented template defaults.
- `backendContractEnabled` — tri-state gate for the backend API contract tooling (the `orchestrator/api-contract/` sidecar + the contract-aware data-layer flow). See the `backend-contract-client` skill. One of:
  - `auto` (default) — agents use the validated current generation when it exists; `backend-contract-drift` may report `SKIPPED (no snapshot)` for unrelated greenfield work, but endpoint/DTO work is `BLOCKED` until Backend Test + Refresh publishes a snapshot. Task text is never a substitute for the server contract.
  - `true` — require the snapshot: `endpoint-builder` stops (`BLOCKED`) when an endpoint is missing from the inventory instead of guessing; the drift validator must run.
  - `false` — disable the contract tooling: `launch.md` Step 6.6 is skipped and the contract validator no-ops.
Backend credentials never belong in this file. Bearer tokens or Postman API keys are guarded local files under `orchestrator/api-contract/.secrets/<environment-id>.token`; the manifest stores only `authRef` and `authKind`.

## Updating

When a value changes (new locale added, app ships and `prelaunch` flips, codex installed), update this file. The skills read it lazily — no rebuild needed.
