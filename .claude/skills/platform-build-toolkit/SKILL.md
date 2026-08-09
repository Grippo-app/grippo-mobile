---
name: platform-build-toolkit
description: >-
  Build-system and platform-utility layer for the KMP template — the bottom of the
  dependency graph. Use when a task touches a toolkit module, a build, Gradle, a
  convention plugin, the version catalog, settings.gradle, a compose-lib widget, the
  app shell, screenshot infra, or build/test commands. Covers tech-stack versions,
  :toolkit:* utilities (context, http-client, serialization, logger, date-utils,
  connectivity, notification-manager, permission-manager, link-opener, image-loader,
  theme, localization), :compose-libs:* reusable widgets, build-logic convention
  plugins, the version catalog, settings.gradle, representative build.gradle.kts
  shapes, the iOS SwiftPackage framework, the :iosApp Xcode project, and the thin
  :androidApp/:iosApp shells. Routes to the toolkit-builder, compose-lib-builder, and
  app-shell-builder agents.
---

# platform-build-toolkit

The build-time foundation: language/version baseline, the version catalog and
convention plugins, the `:toolkit:*` utility layer, `:compose-libs:*` reusable
widgets, the representative `build.gradle.kts` shapes, the iOS framework/project,
and the two thin app shells — see `references/index.md`.

## When to use

- Add or extend a `:toolkit:<name>` utility (dates, logging, connectivity, permissions, …) — route to **toolkit-builder**.
- Add or extend a `:compose-libs:<name>` product-agnostic widget — route to **compose-lib-builder**.
- Edit the thin `:androidApp`/`:iosApp` shells (permission, intent filter, deeplink filter, platform-service wiring, root-ctor) — route to **app-shell-builder**.
- Author a module's `build.gradle.kts`, add a catalog entry/plugin, touch `settings.gradle.kts`, build-logic convention plugins, or the iOS framework/project.
- Decide languages/versions, gradle.properties, or run build/test gates.

NOT this skill: domain logic, data services/repos/mappers, DI modules, UI screens/MVI, design-system token-styled components — those route to their own skills.

## Required inputs

- **Task file** (`orchestrator/tasks/todo/TASK_*.md`) and, if present, the **Implementation plan** (planner output) — its per-builder file list and naming table win on target conflicts; this skill's methodology wins on pattern conflicts.
- **Project config** (`orchestrator/project-config.md`): `iosEnabled`, `iosFrameworkName`, `firebaseEnabled`, `org`, `productPackage`, `applicationId`.
- For a builder: **mode** (`new`/`extend`), **module name** (kebab-case), the **API surface** to add, and any **consumer call site**.

If a required reading file or config field is missing, stop and report `BLOCKED: required reading missing — <list>` (or the agent's specific BLOCKED string). Do not proceed on assumed content.

## Workflow

1. **Toolkit-first ordering.** The dependency graph is built bottom-up: toolkit utilities and build-logic exist before the layers that consume them; the app shells are wired **last** and stay thin. A `:toolkit:*` module depends on (almost) nothing; the closed rule-5 exceptions (`http-client → error-provider`; `date-utils → design-system resources`) are not extendable.
2. **Pick the layer.** Toolkit (platform-aware, product-agnostic util) vs `:compose-libs` (reusable widget, all style/data as parameters, no `AppTokens`) vs shell (platform wiring only). Wrong layer → refuse with the agent's BLOCKED string, don't force-fit.
3. **Convention plugins.** Modules apply `android.library.convention` + `kotlin.multiplatform.convention` (+ `compose.multiplatform.convention`, `koin.annotation.convention`, serialization plugin, `screenshot.test.convention` only when actually used). `:androidApp` applies the Compose plugins directly (single-target). Never re-declare `compileSdk`, `repositories`, or toolchain in a module — that lives in the convention plugin.
4. **Catalog discipline.** All versions come from the version catalog — no inline version strings in module scripts, one version per artifact group, no stale entries, no in-flight key renames without updating every consumer. A brand-new third-party library/plugin is a **stop-and-ask** (must be authorized in the task text).
5. **App-shell-last & shell-only.** Shells (1) init DI (`Koin.init`/`Koin().doInit`), (2) start the Decompose `RootComponent`, (3) wire platform services. No business logic, mappers, screens, or Koin modules. The `:iosApp` `project.pbxproj` is verbatim — shape-copy from the fences, never hand-author the UUID graph.
6. **Build/test gates.** Verify green before reporting done (see Validators / gates). The iOS XCFramework gate is what catches a missing `actual`; skip every iOS gate when `iosEnabled: false`.

## Stop and ask

- New third-party dependency (Gradle/SPM/npm) or any build-config change (convention plugin, `settings.gradle`, pbxproj phase) not authorized in the task text.
- Wrong layer (capability belongs to a data-service / design-system / feature module).
- `new` mode but the module already exists, or an existing module already covers the capability.
- Catalog key rename that breaks consumers; rule-5 exception-list extension.
- An `expect` without both `actual`s; a Koin module pushed into a stateless helper or a shell.
- pbxproj change that exceeds a clean verbatim shape-copy (needs macOS/Xcode authoring).

## References to read

This skill is self-contained: its owned build-time rules live in
`references/**`. Read the agent's own `## Authoritative reading`
order first, then the reference file for your topic. Start at the routing table:
**`references/index.md`** (topic → file). Key entrypoints:

- Tech stack (versions, `libs.versions.toml` catalog, `gradle.properties`): `references/tech-stack.md`.
- Module graph & dependency rules: `references/module-structure.md`.
- Toolkit module layout, build-logic / convention-plugin modules, toolkit overview: `references/toolkit-modules.md`.
- `:toolkit:*` per-module specs: `references/toolkit-utilities-core.md` (context, http-client, serialization, logger, date-utils) and `references/toolkit-utilities-platform.md` (connectivity, notification-manager, permission-manager, link-opener, theme, localization, image-loader); recipe `references/toolkit-cookbook.md`.
- Convention plugins: `references/convention-plugins.md`.
- Version catalog + `settings.gradle.kts`: `references/version-catalog-and-settings.md`.
- Module builds (`:androidApp`, `:shared`, representative shapes): `references/gradle-build.md`.
- iOS framework / XCFramework: `references/ios-framework.md`; `:iosApp` Xcode project (verbatim fences): `references/ios-app-project.md`.

Not packed here (read via the relevant agent's `## Authoritative reading`): `:compose-libs:*` module rules (compose-lib-builder) and the thin-shell mandate + shared-composition-root ctor (app-shell-builder).

Full per-topic routing: `references/index.md`.

## Validators / gates

- **architecture-validator** — module-graph dependency direction; every `implementation(projects.*)` edge must be allowed by `references/module-structure.md` § directional dependency rules (toolkit at the bottom; no forbidden edges).
- **build-validator** — `:<module>:assemble`, the `:shared:assemble<IosFrameworkName>DebugXCFramework` gate when `iosEnabled: true`, and `:androidApp:assembleDebug` must build green; captures compiler warnings (deprecation discipline).
- **anti-pattern-scanner** — inline version strings, `apply(plugin=…)`, in-module `repositories {}`/`compileSdk`, unauthorized new deps, forbidden plugin shapes (no-extra-deps gate).

## Output contract

Builders return the envelope in `orchestrator/contracts/builder-report.md`
(`agent, status ∈ done|blocked|failed|skipped, files_touched, produced_signatures,
blockers, scope_deviations`; `blocked|failed` ⇒ non-empty `blockers`; out-of-scope
writes are a `scope-leak-validator` finding, not a silent entry). When an
Implementation plan drives the task, its targets come from
`orchestrator/contracts/planner-output.md`.
