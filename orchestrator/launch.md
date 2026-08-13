# Launch — Bootstrap a New Project from These Requirements

This prompt is fed to an agent (Claude / GPT / etc.) at the **root of an empty directory** to scaffold a new KMP mobile project that conforms to the architecture carried by the installed skills (under `orchestrator/skills/`, deployed to `.claude/skills/`). The agent works **iteratively**, building one layer at a time, verifying each layer compiles before moving on.

Run the prompt **once** at the start of a new project. Re-running it later is unnecessary — the per-skill cookbook recipes (carried by each owning skill, e.g. `data-layer`, `ui-feature`, `design-system`) cover ongoing growth.

---

## How to use

1. Copy the `orchestrator/` folder plus the root `package.json`,
   `package-lock.json`, `.npmrc`, and `.nvmrc` to the new project's root.
2. Run `npm ci` once from the new project root.
3. Feed the prompt below to your agent.
4. Answer the agent's clarifying questions about product name, org, package, etc.
5. The agent will create modules incrementally and verify each layer with `./gradlew` builds.
6. After the agent completes the foundation, follow the cookbook to add features.

> When you see `Note*` / `Tag*` / `User*` identifiers in example code blocks across the docs, treat them as `<Entity>` / `<RelatedEntity>` placeholders — they are illustrative, not architectural. Replace with your product's domain types.

---

## The prompt

````
You are bootstrapping a new Kotlin Multiplatform (KMP) mobile project from scratch. The architecture is fully specified by the skills under `orchestrator/skills/` (each `SKILL.md` + its `references/`), which are deployed into `.claude/skills/` in Step 14. Read the owning skill for each layer before building it — they are normative. The `launch-readiness` skill carries the project-overview and the bootstrap field-mapping; the per-layer skills (`platform-build-toolkit`, `ui-feature`, `design-system`, `data-layer`, `mappers`, `di-modules`, `backend-contract-client`, `implement-figma`, `validation-gates`) carry the rules for their respective layers.

Goal: produce a project that builds green on both Android and iOS (XCFramework) and has a working "hello world" screen wired through the full stack (MVI, Decompose, Koin, design system, data layer).

## Unattended run — never ask

After Step 0, the bootstrap runs unattended, start to finish. Never stop to ask the user anything, and never end a step, a turn, or the final report with a question or an offer ("Want me to…?", "Should I commit…?"). Concretely:

- Decision points resolve by the documented defaults in these steps; record every such decision in the step report instead of asking.
- Fixes required to make a step's verification pass are in scope: apply them, re-run the check, and list them in the step report. Leave them in the working tree with the rest of the bootstrap output — never ask whether to apply, commit, or keep them.
- A genuinely missing required input is a failure, not a question: stop with `BLOCKED: <what is missing>` and report it.
- The validation-gates skill's when-to-stop-and-ask reference governs post-bootstrap task work. During bootstrap it never licenses a question — its analogue here is the `BLOCKED:` report.
- In the site flow, `orchestrator/project-config.md` is written by the Setup panel before any step runs — every Step 0 answer is already there; there is nothing to ask.

## Step 0 — gather context

Resolve the following. If `orchestrator/project-config.md` is already populated (the site Setup panel writes it before any step runs), read every answer from there and ask nothing. Only in the manual flow, with no populated config, gather them via clarifying questions — Step 0 is the only step of the bootstrap allowed to ask the user anything (see "Unattended run — never ask"):

1. **Product name** (e.g. `SampleApp`). Fills the `<product>` / `<Product>` placeholders everywhere — package root, root project name, namespace prefix.
2. **Organization name** (e.g. `example`). Used for the package root: `com.example.sampleapp.*`. Skip if single-org (`com.sampleapp.*`).
3. **Backend host** (e.g. `api.example.com`) for `BackendClient.defaultRequest.host`. If not yet known, stop here and ask for a concrete temporary host; do not leave a source placeholder in `orchestrator/project-config.md`.
4. **Application ID** for Google Play (e.g. `com.example.sampleapp`). Convention: `com.<org>.<product>` (same as the package root; reused verbatim as the iOS bundle id — no platform suffix).
5. **First product domain** to model — minimal end-to-end feature (e.g. "Notes", "Tasks", "Bookmarks"). One entity, one feature, one screen.
6. **Locales to support** (e.g. `en`, `uk`). At least one (`en`).
7. **Auth methods** (any combination of: email/password, Google Sign-In, Apple Sign-In, or skip for now).
8. **Firebase** — enabled or skipped? (If enabled, the agent will scaffold the placeholders but leave actual `google-services.json` for the user.)

### Field mapping

| Step 0 question                | project-config.md field           |
|---|---|
| Product name                   | productName, productPackage (lowercased), apiClassName (productName + "Api") |
| Organization name              | productPackage (com.<org>.<product>) |
| Backend host                   | backendHost |
| Application ID                 | applicationId |
| First product domain           | recorded durably (see note below) — seeds the first feature in Step 8 |
| Locales to support             | supportedLocales (YAML list) |
| Auth methods                   | diHandWrittenModules (append GoogleAuthModule / AppleAuthModule per choice) |
| Firebase                       | firebaseEnabled |

Record the **first product domain** (q5) and the **auth methods** (q7) answers durably, not in agent memory — a bootstrap spans hours and may cross sessions. Either persist them in `orchestrator/project-config.md` (auth already lands in `diHandWrittenModules`; add the first domain as a `# bootstrap-first-domain: <X>` config comment) or write both into a scratch block in the bootstrap task. Step 3's auth-conditional scaffolding and Step 8's first-domain seed read from that durable record.

Defaults for fields the user is not asked about but MUST be set:

- `iosEnabled` — default `true`; false only if the project intentionally drops iOS.
- Reviewer mode — default **Automatic** (stored in the `codexEnabled` backing field).
- `prelaunch` — default `true` for a fresh project (room-migration-builder allows destructive fallback). Flip to `false` when the app ships.
- `iosFrameworkName` — default `shared`.
- `typefaceFactory` — name the project picks for its typeface factory function (e.g. `inter`, `roboto`). Default placeholder `<typeface>` until decided.
- `featuresWithRootComponentSuffix` — start as `[]`; the orchestrator updates it when a feature is forced into the suffixed form.
- `verifyEnabled` — default `auto`. Orchestrator's runtime-verify gate (Step 4.6) auto-detects the Anthropic `verify` skill at runtime. Force `true` for CI-strict mode or `false` for headless CI where the app cannot be launched. Note: the runtime-verify (Step 4.6) and conditional security-review (Step 5.5) gates only fire when the **parent session that adopts the orchestrator playbook** has the `Skill` tool enabled — editing any skill frontmatter does not change the already-adopted role for that run.
- `figmaEnabled` — default `false`. Set `true` only when the project has a Figma design library to bind; enables Step 6.5 and the Figma validators. For a task with a non-`none` `## Design` bullet, the screenshot-fidelity gate is mandatory: missing oracle/capture evidence is a BLOCKER (any theme, no separate enable flag). Leave `false` for a non-Figma project.
- `backendContractEnabled` — default `auto`. Agents use only a validated contract generation; a missing snapshot does not block unrelated bootstrap work, but endpoint/DTO work must wait for Backend Test + Refresh instead of guessing from task text.
- `figmaLibraryUrl` — keep the `<figma-library-url>` placeholder at bootstrap; filled later in Step 6.5 when `figmaEnabled` is set. This project-config field is the sole canonical Figma file URL.
Save the answers AND copy them into `orchestrator/project-config.md` per the field-mapping table above in Step 1.5. The answers are not just a reference — they become the runtime configuration every skill reads.

## Step 1 — read the skills

The architecture is carried by the skills under `orchestrator/skills/<name>/` (each `SKILL.md` + its `references/`). Read the owning skill for each layer — `SKILL.md` first, then its `references/` — in this order, because the layers build on one another:

- `launch-readiness` — project overview, architecture overview, glossary, conventions, and the bootstrap field-mapping.
- `platform-build-toolkit` — tech stack + version catalog + gradle properties, convention plugins, the module graph and representative builds, the `:toolkit:*` utilities, and the `:androidApp` / `:iosApp` shells (incl. the iOS SwiftPackage / Xcode project).
- `ui-feature` — base classes, MVI / Decompose architecture patterns, the error pipeline, `:ui-core:state` models and formatters, and the dialog navigation contract.
- `design-system` — tokens / theme / components / previews / resources.
- `data-layer` — data sources, repositories, DTOs, Room, DataStore, backend client.
- `mappers` — the seven directional `:data-mappers:*` modules.
- `di-modules` — Koin module wiring and the composition root registration.
- `validation-gates` — naming conventions, forbidden / anti-patterns, and the when-to-stop-and-ask rules.
- `implement-figma` — Figma → Compose workflow (only if `figmaEnabled: true`).
- `backend-contract-client` — the committed contract snapshot discipline (only if `backendContractEnabled` is not `false`).

The 11 core skills under `orchestrator/skills/` are deployed to `.claude/skills/` in Step 14; until then read them in place. `implement-figma` is read and deployed separately from `orchestrator/figma/skill/` in Step 6.5 when `figmaEnabled: true`. Don't skim. The architectural rules are interconnected.

## Step 1.5 — populate project-config.md

Open `orchestrator/project-config.md`. Replace every required identity/build value in the YAML frontmatter with the project-specific value from Step 0, per the field-mapping table. For fields the user did not specify, use the defaults noted in Step 0's mapping table. Leave the Figma placeholder until that integration is configured.

Verify:
- No required identity/build value in the frontmatter still reads `<Product>`, `<product>`, `<org>`, `<product-domain>`, or any other required placeholder.
- The deferred `figmaLibraryUrl` integration placeholder may remain.
- `featuresWithRootComponentSuffix` and `diHandWrittenModules` are empty `[]` or contain only modules the project actually intends to ship.
- `prelaunch: true` for an unreleased project; `firebaseEnabled` matches the Step 0 answer.

Do NOT proceed to Step 2 until `orchestrator/project-config.md` is fully populated. Every skill invoked later reads this file.

## Step 2 — initialize the build system

When copying verbatim from the `platform-build-toolkit` skill (its tech-stack and gradle-build references), substitute every `<org>`, `<product>`, `<Product>`, `<iosFrameworkName>`, and `<IosFrameworkName>` placeholder with the value from `orchestrator/project-config.md` that you populated in Step 1.5. Verbatim refers to structure, not to placeholder tokens.

`<IosFrameworkName>` = `iosFrameworkName` with the first letter upper-cased (`shared` → `Shared`); it must match the Gradle task name `assemble<IosFrameworkName>DebugXCFramework`. The module path itself is always `:shared:` per the project's iOS convention.

Create the following in order:

1. **Generate the Gradle wrapper.** A host Gradle install (9.1.0+) must be available on the PATH. Run `gradle wrapper --gradle-version 9.1.0` (AGP 9.0.1 requires Gradle >= 9.1.0 — AGP's own version check fails the build on anything older). This produces:
   - `gradle/wrapper/gradle-wrapper.jar`
   - `gradle/wrapper/gradle-wrapper.properties`
   - `gradlew` (Unix launcher script)
   - `gradlew.bat` (Windows launcher script)
   On an empty directory without a system Gradle install, `./gradlew` does not exist — this step is the prerequisite for every later `./gradlew` invocation. Install the required Gradle version temporarily if it is not already available; do not copy wrapper artifacts from another project. Confirm `gradle/wrapper/gradle-wrapper.properties` points at the intended distribution URL.
2. **`gradle/libs.versions.toml`** — verbatim from the `platform-build-toolkit` skill's version-catalog reference, with versions left unchanged. Adjust catalog keys if you renamed the product.
3. **`gradle.properties`** — verbatim from the `platform-build-toolkit` skill's gradle-properties reference.
4. **`settings.gradle.kts`** — root settings file. Initially include ONLY:
   ```
   include(":androidApp")
   include(":shared")
   ```
   You will add more modules incrementally.
5. **`build-logic/`** with the convention plugins from the `platform-build-toolkit` skill's convention-plugins reference. Create:
   - `build-logic/settings.gradle.kts`
   - `build-logic/build.gradle.kts`
   - `build-logic/convention/build.gradle.kts` with the `gradlePlugin { plugins { ... } }` registrations.
   - `build-logic/convention/src/main/kotlin/<Name>ConventionPlugin.kt` for each convention plugin — the seven core plugins, the opt-in general test foundation (`KmpTestConventionPlugin` plus the capability plugins `Coroutines/Flow/Network/Di/Room/ComposeUi TestConventionPlugin` and `TestCapabilityEntryTask`, all registered but applied only by test-bearing modules), and the optional, inert `ScreenshotTestConventionPlugin.kt` (applied by no module until the screenshot gate is enabled; see the `platform-build-toolkit` skill's convention-plugins reference § "Test conventions" and § "Optional convention plugin").
   - `build-logic/convention/src/main/kotlin/com/<org>/{PluginManagerExtensions.kt, ConfigureJvmToolchain.kt, ProjectExtensions.kt}` (canonical helper names — see the toolkit-modules reference § Structure).
6. **Root `build.gradle.kts`** — verification aggregates only (`allHostTests`, `allIosSimulatorTests`, `allAndroidDeviceTests`, `allScreenshotTests`, `allConfiguredTests`, `testCapabilityInventory`), verbatim from the `platform-build-toolkit` skill's version-catalog reference § "Root build.gradle.kts". It applies no plugins and configures no modules.
7. **Run** `./gradlew --version` to verify the wrapper resolves. Expected: the pinned Gradle version (9.1.0). The reported JVM is the host launcher's, NOT the project's `jvmToolchain(19)` target — that JVM 19 is provisioned per-module by the toolchain and is not what this command reports. The host JVM only needs to satisfy AGP's minimum (JDK 17+). If this fails with "no such file or directory", step 1 was not completed — the wrapper jar and launcher scripts are missing.

## Step 2.5 — write the root `.gitignore`

Create `.gitignore` at the repo root with at minimum:

```
build/
*/build/
/build-logic/convention/bin/
.gradle/
.idea/
*.iml
*.iws
local.properties
.DS_Store
*.hprof
hs_err_pid*
replay_pid*
*.log
.kotlin/
kotlin-js-store/
captures/
.externalNativeBuild/
.cxx/
**/schemas/
xcuserdata/
*.xcuserstate
*.xcworkspace
*.pbxuser
*.mode1v3
*.mode2v3
*.perspectivev3
*.moved-aside
DerivedData/
Pods/
Podfile.lock
*.generated.swift
**/generated/
.AppleDouble
._*
Thumbs.db

# BEGIN ORCHESTRATOR RUNTIME IGNORE CONTRACT
# Orchestrator site + Figma + api-contract tooling — local secrets + runtime artifacts (never commit)
.claude/settings.local.json
node_modules/
.env
__pycache__/
*.pyc
orchestrator/figma/.account.json
orchestrator/api-contract/.secrets/
# Consolidated cache root (gitignored): tasks locks/requests/worker/runs/journal,
# figma + api-contract caches & reports, and site state — all created at runtime (mkdir -p).
orchestrator/.cache/
# Atomic finalizer proofs must share the task filesystem but are runtime-only.
orchestrator/tasks/todo/.finalize-*.ship
orchestrator/tasks/todo/.finalize-*.ship.tmp.*
orchestrator/tasks/todo/.finalize-*.detach.md
# END ORCHESTRATOR RUNTIME IGNORE CONTRACT
```

Figma tooling lives under `orchestrator/figma/`, but its regenerable artifacts now live under
`orchestrator/.cache/figma/` (screens, derive matrices, token caches, reports). The Figma tab
(`orchestrator/site/`) writes the bound-account identity to `orchestrator/figma/.account.json` — a
secret that STAYS at the sidecar root. Both it and the whole `orchestrator/.cache/` tree MUST stay
out of git (and the site's static server denies them — see `server/static.js`: the dotfile denylist
refuses any path with a leading-dot segment, which covers `.account.json` AND the dot-prefixed
`.cache/` root). The two `.finalize-*` todo patterns are the finalizer's private no-clobber/
detachment proofs; its durable marker reports any conflict, and the commit watcher must never
stage those runtime names. Design context comes only from the OAuth-bound MCP — there is no REST/token
fallback. Curated `orchestrator/figma/tokens/`, `manifests/`, `scripts/`, and `token-schemas/` ARE
committed (design-system inputs + tooling). The Figma entries only apply when `figmaEnabled: true`;
a non-Figma project never creates these paths.
The api-contract sidecar follows the same rule: per-environment opaque credentials
(bearer tokens or Postman `PMAK-` API keys, selected by the manifest `authKind`)
(`orchestrator/api-contract/.secrets/<environment-id>.token`) plus the regenerable
caches/reports under `orchestrator/.cache/api-contract/` (raw spec/Postman dumps,
per-run drift output) MUST stay out of git; the static server refuses all dot-prefixed segments and
`.cache/`. Committed for that sidecar: current generation manifests/artifacts,
`contract-schemas/`, and `scripts/`. The api-contract entries apply when
`backendContractEnabled` is not `false` (default `auto`) — a project with the tooling off never
creates these runtime paths.

Verify: `git status --ignored` shows `.idea/` (already created by Android Studio) and `build/` (created by Gradle in Step 2), plus any already-present gitignored runtime artifacts (`orchestrator/.cache/`, `node_modules/`, the sidecar secret files) — none of which should be tracked. `.claude/skills/` contains the skills installed in Step 14; verify it separately without assuming it is gitignored.

## Step 3 — scaffold the foundation modules

In one batch, create the remaining infrastructure modules from the `platform-build-toolkit` skill's module-graph reference § "Mandatory infrastructure modules". The reference lists 38 mandatory-infrastructure modules (35 created here + 3 handled in other steps) plus 2 optional auth modules (`google-auth`, `apple-auth`); this step creates the 35 — `:androidApp`'s `include()` line is added in Step 2 (root project setup) and the module itself is implemented in Step 10; `:shared` is created in Step 9 (composition root); `:ui-dialog-features:error-display` ships real code and is built in Step 7.7 (see the chapter's callout); all three are intentionally excluded from the batch below. Of the 35, `:data-services:firebase` is conditional on `firebaseEnabled`.

For each:

1. Create the directory.
2. Create the `build.gradle.kts` (representative-builds template from the `platform-build-toolkit` skill). **Carve-out:** `:design-system:resources:provider` and `:toolkit:notification-manager` must NOT use the representative-builds template (the representative-builds reference forbids them) — these two ship `composeResources/`/androidMain `res/`, so use the full design-system-module build script from the `platform-build-toolkit` skill's design-system-modules reference and add `androidLibrary { androidResources.enable = true }` per the `design-system` skill's resources reference § Build requirement; without it composeResources are not packaged on AGP 9 and the app crashes at runtime with `MissingResourceException`.
3. Create an initial `src/commonMain/kotlin/com/<org>/<product>/<area>/<module>/.gitkeep` (empty package marker).
4. Add `include(":<group>:<name>")` to `settings.gradle.kts`.

Modules to scaffold (in this order, infrastructure first):

- `:toolkit:context`
- `:toolkit:logger`
- `:toolkit:serialization`
- `:toolkit:date-utils`
- `:toolkit:http-client`
- `:toolkit:theme`
- `:toolkit:localization`
- `:toolkit:connectivity`
- `:toolkit:image-loader`
- `:toolkit:link-opener`
- `:toolkit:notification-manager`
- `:toolkit:permission-manager`
- `:ui-core:foundation`
- `:ui-core:state`
- `:ui-core:error:error-provider`
- `:ui-core:error:error-provider-impl`
- `:design-system:resources:provider`
- `:design-system:resources:provider-impl`
- `:design-system:core`
- `:design-system:components`
- `:design-system:preview`
- `:data-services:datastore`
- `:data-services:database`
- `:data-services:backend`
- `:data-services:firebase` (if Firebase is enabled)
- `:data-services:google-auth` (if Google Sign-In was selected in Step 0)
- `:data-services:apple-auth` (if Apple Sign-In was selected in Step 0)
- `:data-features:feature-api`
- `:data-mappers:dto-to-entity`
- `:data-mappers:entity-to-domain`
- `:data-mappers:dto-to-domain`
- `:data-mappers:domain-to-state`
- `:data-mappers:state-to-domain`
- `:data-mappers:domain-to-entity`
- `:data-mappers:domain-to-dto`
- `:ui-screen-features:screen-api`
- `:ui-dialog-features:dialog-api`

Verify after the full Step-3 batch: `./gradlew tasks` must succeed without errors. Run it only once ALL 35 `include(...)` lines are in `settings.gradle.kts` — the representative-builds templates declare `implementation(projects.*)` for sibling modules, and those type-safe `projects.*` accessors only exist once each module is registered in `settings.gradle.kts`; running `./gradlew tasks` mid-batch fails on unresolved accessors. (Alternative: strip the inter-module `implementation(projects.*)` lines from the empty scaffold and let the later impl steps re-add them as each dependency comes online.)

## Step 4 — implement the base classes

Implement `:ui-core:foundation` from the `ui-feature` skill's base-classes references:

- `BaseViewModel`, `BaseComponent`, `BaseComposeScreen`, `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult`, `ResultKey`, `ResultKeys`, `ComponentIdentifier`, `NoneIdentifier`.
- `OperationManager` (interface + impl).
- `ResultManager` + `ResultEmitter`.
- `collectAsStateMultiplatform` (expect/actual).
- `platformAnimation` + `platformStackAnimator` (expect/actual).
- `CoreModule` (`@Module @ComponentScan`).

Each base-classes reference in the `ui-feature` skill ends with a `## Reference implementation` section containing the drop-in source — copy each `### <basename>.kt` kotlin fence verbatim into the project at the target path noted under each subheading. Do NOT reconstruct from the prose API descriptions above the Reference implementation section — they are the contract, not the source. Substitute `<org>` / `<product>` placeholders with the concrete package values from `orchestrator/project-config.md` (the tokens appear both inside the kotlin fences and in each `Target path inside the module:` line). For chapter 01 (`BaseViewModel.kt`), respect the `// region firebase-conditional` / `// endregion firebase-conditional` markers per the gate note above the fence — if `firebaseEnabled = false`, strip everything between the markers (both the import and the call site).

Verify (deferred — see Step 4.5): `./gradlew :ui-core:foundation:assemble` builds AFTER Step 4.5 seeds the error contracts. At this step, only check that the source files compile (look for unresolved-reference errors against `ErrorProvider` — those resolve in Step 4.5).

> **Order note.** `BaseViewModel` injects `ErrorProvider` (from `:ui-core:error:error-provider`). Implement Step 4.5 immediately after Step 4 so the deferred `:ui-core:foundation:assemble` check can pass. Additionally, when `firebaseEnabled: true`, `BaseViewModel` references `FirebaseProvider` (from `:data-services:firebase`, a dependency of `:ui-core:foundation`), which is only authored in Step 7 — so the `:ui-core:foundation:assemble` re-check cannot pass until then either. Defer this specific re-check to Step 7, exactly as the `ErrorProvider` part is deferred to Step 4.5.

## Step 4.5 — implement the error contracts

Step 3 scaffolded `:ui-core:error:error-provider` and `:ui-core:state` as empty `.gitkeep` directories. Seed the error types in both now — `:ui-core:foundation` (Step 4), `:toolkit:http-client` (Step 5 — `HttpResponseValidator` throws `AppError.Network.*`), and `:ui-dialog-features:dialog-api` (Step 7.5 — `DialogConfig.ErrorDisplay` references `AppErrorState`) all depend on them.

Authoritative reading: the `ui-feature` skill's ui-core-modules reference § "`:ui-core:error:error-provider`" and § "`:ui-core:state`", plus its error-pipeline reference.

### `:ui-core:error:error-provider` — `AppError` + `ErrorProvider` only

Inside `src/commonMain/kotlin/com/<org>/<product>/core/error/provider/`:

- `AppError.kt` — `public sealed class AppError(message, cause) : Exception(message, cause)` with `Network.{NoInternet, Timeout, Expected, Unexpected}`, top-level `Expected(message, description)`, and `Unknown : AppError(null)`. The full subtype list and constructor signatures live in the `ui-feature` skill's error-pipeline reference — copy verbatim.
- `ErrorProvider.kt` — `public interface ErrorProvider { public suspend fun provide(exception: Throwable, callback: () -> Unit) }`. Interface only — the impl lives in `:ui-core:error:error-provider-impl` and isn't bootstrapped here (the cookbook wires it together with the error pipeline).

The module is **deps-free** in `build.gradle.kts` — no `sourceSets.commonMain.dependencies { ... }` block. This is intentional per the `ui-feature` skill's ui-core-modules reference: `:data-services:*` and `:toolkit:http-client` depend on `AppError` without dragging in any UI surface.

### `:ui-core:state` — seed `AppErrorState`

`:ui-core:state` is a large module (eventually holds `UiText`, `*FormatState`, product-specific reusable state). The bootstrap only needs **one type** from it before later steps compile: `AppErrorState`. Add the rest as features arrive — this step is a partial seed, not the full implementation.

Inside `src/commonMain/kotlin/com/<org>/<product>/core/state/error/`:

- `AppErrorState.kt` — `public sealed class AppErrorState` mirroring `AppError` for UI display, with `Network.{NoInternet, Timeout, Expected, Unexpected}`, top-level `Expected`, and `Unknown` (singleton). Constructor params are per-subtype, mirroring what `ErrorProviderImpl` passes (the `ui-feature` skill's error-pipeline reference): `Network.NoInternet(description: String?)`, `Network.Timeout(description: String?)`, `Network.Expected(title: String, description: String?)`, `Network.Unexpected(description: String?)`, top-level `Expected(title: String, description: String?)`, and `Unknown` as a singleton. Plain strings — no `UiText` wrapping in the bootstrap shape.

The `:ui-core:state/build.gradle.kts` template in the `ui-feature` skill's ui-core-modules reference applies as-is.

### Verify

`./gradlew :ui-core:error:error-provider:assemble :ui-core:state:assemble` builds. Re-run `./gradlew :ui-core:foundation:assemble` to confirm the deferred Step 4 verification now succeeds (ErrorProvider/FirebaseProvider-dependent parts deferred to their creation step — `FirebaseProvider` lands in Step 7, so when `firebaseEnabled: true` the full foundation assemble only passes there).

## Step 5 — implement the toolkit

Implement each `:toolkit:*` module from the `platform-build-toolkit` skill's toolkit references:

- `:toolkit:context` — `NativeContext` expect/actual, `ContextModule`.
- `:toolkit:logger` — `AppLogger`.
- `:toolkit:serialization` — `Json` provider via `SerializationModule`.
- `:toolkit:date-utils` — `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting`.
- `:toolkit:http-client` — base `HttpClient` + `ApiErrorParser`.
- `:toolkit:theme`, `:toolkit:localization` — `AppTheme.current`, `AppLocale.current` expect/actual.
- `:toolkit:connectivity` — `Connectivity` interface + Android/iOS impls.
- `:toolkit:notification-manager`, `:toolkit:permission-manager`, `:toolkit:link-opener` — minimal stub impls. Reminder: `:toolkit:notification-manager` ships androidMain `res/`, so its `build.gradle.kts` must be the full design-system-module build script (`platform-build-toolkit` skill's design-system-modules reference) with `androidLibrary { androidResources.enable = true }` per the `design-system` skill's resources reference § Build requirement — not the representative-builds template (see the Step 3.2 carve-out).
- `:toolkit:image-loader` — Coil 3 + Ktor 3.

Verify after each: the module builds and the Koin module is annotated.

## Step 6 — implement the design system

Implement `:design-system:resources:provider`:

Reminder: this module ships `composeResources/`, so its `build.gradle.kts` must be the full design-system-module build script (`platform-build-toolkit` skill's design-system-modules reference) with `androidLibrary { androidResources.enable = true }` per the `design-system` skill's resources reference § Build requirement — not the representative-builds template (see the Step 3.2 carve-out).

- `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon` interfaces/objects.
- `LocalAppColors`, `LocalAppDp`, ... CompositionLocals.
- `StringProvider` interface.
- Initial `strings.xml` in `commonMain/composeResources/values/strings.xml` with one key (e.g. `app_name`).
- For every locale in `supportedLocales` beyond `en`, also create `commonMain/composeResources/values-<lang>/strings.xml` with the same key (the `design-system` skill's resource workflow later requires every `supportedLocales` entry to receive each key — see `orchestrator/project-config.md`; there is no bootstrap-time gate for this).
- Initial drawable, e.g. `ic_back.xml`.

Implement `:design-system:resources:provider-impl`:

- `StringProviderImpl` + `ResourcesProviderModule`.

Implement `:design-system:core`:

- `AppTokens`.
- `AppTheme` + `ProvideResources`.
- `LightAppColors` + `DarkAppColors` with a minimal but complete set of color slots.

Implement `:design-system:components` (minimal):

- `Toolbar`, `BottomSheetToolbar`, `Button`, `BaseComposeScreen`-style widgets.

Implement `:design-system:preview`:

- `@AppPreview` multi-preview annotation.
- `PreviewContainer` composable.
- `PreviewContainerScreenshot` — a full-bleed sibling of `PreviewContainer` (same `AppTheme` + Coil placeholder handler, but a `Box(Modifier.fillMaxSize())` with no padding). Ship it now even though it is inert until the screenshot gate runs (figmaEnabled + a non-`none` `## Design` screen + a pulled oracle); the screenshot-fidelity gate renders screens through it (parameterise its `darkTheme` to match your AppTheme's theme axis). See the validation-gates skill's `screenshot-fidelity-gate.md` reference.

Keep the module roots and the `AppColor`, `AppDp`, `AppTokens`, `LightAppColors`, and
`DarkAppColors` filenames above as the standard bootstrap contract. When Figma is enabled, the first
Sync derives `orchestrator/figma/project-adapters.json` from this layout automatically; the user does
not create that file.

The typeface factory function for this project is `<typeface>` — wire it through `resource-builder` conventions when fonts are added.

## Step 6.5 — Figma tooling sidecar (only if `figmaEnabled: true`)

**Skip this entire step when `figmaEnabled: false` in `orchestrator/project-config.md` (the default).** There is no
existing whole-step-skip precedent in this file — this is an explicit conditional: read `figmaEnabled`
first, and if it is `false`, do nothing here and proceed to Step 6.6. A non-Figma project is byte-for-byte
unaffected.

When `figmaEnabled: true`:

If you change `figmaEnabled` from `false` to `true` while the Site is already
running, restart the Site before using any Figma action. The process pins
applicability at startup so recovery, watchers, and session owners cannot be
partially initialized.

1. The `orchestrator/figma/` sidecar (the Node tooling: `package.json`, `.nvmrc`, `.env.example`,
   `scripts/`, `token-schemas/`, `tokens/`, `manifests/`, `README.md`) is already present — it ships as
   template content under `orchestrator/`, copied wholesale like `orchestrator/site/` (it is NOT a set of
   drop-in fences). Nothing to author here; just confirm it copied.
2. The root `npm ci` installs this workspace from the single committed lock.
   Verify `npm run figma:doctor --workspace=figma-pipeline` passes. (Doctor also checks the enforcement wiring —
   `core.hooksPath` — but only as an ADVISORY `WARN` here,
   because the hooks are wired later at Step 14. The hard check runs post-install: see Step 14's
   `FIGMA_STRICT_WIRING=1` verification.)
3. The sidecar is placed AFTER the design system (Step 6) on purpose: that is the earliest point where real
   `:design-system:components` source exists for the registry to index. It is before the first feature (Step 8)
   because screens built from Step 8 onward consult the Figma MCP + the registry.
4. Bind the Figma library: open the site Figma tab → bind the MCP (OAuth) for `figmaLibraryUrl` — the only
   data path (no REST/token fallback). Needs a Dev/Full seat on Professional+ (Starter = 6 MCP reads/month).
5. Press Sync. The site creates the strict `orchestrator/figma/project-adapters.json` from the standard
   Step 6 Compose layout on the first sync and starts the matching zero-read local comparison after the
   provider snapshot is committed. Existing adapter configs are product-owned and never overwritten.
   Manual adapter authoring is only the escape hatch for a deliberately non-standard design-system layout.
6. Install the `implement-figma` skill: copy/symlink `orchestrator/figma/skill/SKILL.md` to
   `.claude/skills/implement-figma/SKILL.md`. Step 14's `install-skills.sh` does **not** install it (it is
   marked `externalSourceException` in the install manifest and skipped) — this separate copy is what deploys
   it. It is the repeatable Figma→Compose workflow; the permanent rules go in
   `CLAUDE.md` (Step 13, the Figma block).

See the `implement-figma` skill for the full contract. This step writes nothing into the app's Gradle/KMP
modules — the sidecar lives entirely under `orchestrator/figma/` and never enters the build.

## Step 6.6 — Backend contract snapshot (skip only if `backendContractEnabled: false`)

**Skip this entire step when `backendContractEnabled: false` in `orchestrator/project-config.md`.** Same explicit
whole-step conditional as Step 6.5, adapted to the tri-state gate: read `backendContractEnabled` first,
and if it is `false`, do nothing here and proceed to Step 7 — the project behaves exactly as if this
integration did not exist. `auto` (the default) and `true` both run the step; they differ only in how
hard a missing spec fails (point 3 below).

When `backendContractEnabled` is `auto` or `true`:

1. The `orchestrator/api-contract/` sidecar (the Node tooling: `package.json`, `.nvmrc`, `.env.example`,
   `scripts/`, `contract-schemas/`, `spec/`, `manifests/`, `README.md`) is already present —
   it ships as template content under `orchestrator/`, copied wholesale like `orchestrator/figma/` (it is
   NOT a set of drop-in fences). Nothing to author here; just confirm it copied.
2. The root `npm ci` installs this workspace from the single committed lock
   with `ajv` and `yaml`. Verify
   `npm run contract:doctor --workspace=api-contract-pipeline` passes.
3. A fresh template intentionally has no `environments.json` and therefore no implicit Local source.
   Add the first environment in Integrations → Backend; that action creates the canonical non-secret
   manifest and makes the new environment its default. You may later edit it using its exact v1 schema.
   Then use the typed flow:
   - OpenAPI source: `npm run contract:refresh-openapi`;
   - explicit Postman-only bootstrap: `npm run contract:refresh-postman`.
   Each command first runs the same read-only probe as Test source and refreshes only from that current
   preview. A requested refresh is strict under both `auto` and `true`: an unreachable source,
   malformed contract, credential/configuration/revision failure, write conflict, or invalid
   generation stops the command without changing the current generation.
   Postman enrichment is configured as `postmanEnrichmentUrl` on an OpenAPI environment and runs within
   the OpenAPI refresh; it is not a competing contract source. Run `npm run contract:verify` after a
   successful refresh. The generation pointer, manifest, normalized source, inventory, and area slices
   are committed contract artifacts; `.secrets/` and `.cache/` remain local. There is no
   manifest-absent source mode or root snapshot writer.
4. The step sits immediately BEFORE the data layer (Step 7) on purpose: the `:data-services:backend`
   scaffold, the first endpoints, and the mappers (Steps 7–8) are built from the real contract snapshot
   instead of guesses.

See the `backend-contract-client` skill for the full contract. The site Backend tab owns source/auth,
typed probe, preview and refresh; the API panel owns endpoint, drift and coverage work from the resulting snapshot.
This step writes nothing into the
app's Gradle/KMP modules — the sidecar lives entirely under `orchestrator/api-contract/` and never
enters the build.

## Step 7 — implement the data layer

> This step is the launch-time equivalent of the data-layer skill's data-service scaffolding rules. Keep the two shapes in sync when either changes.

`:data-services:datastore`:

- `DataStore<Preferences>` provider with `expect/actual` paths.

`:data-services:database`:

- `Database` class with `@Database(version = 1)` and a minimal entity (e.g. `TokenEntity` + `UserActiveEntity` for the auth subsystem).
- `DatabaseBuilder` expect/actual.
- `DatabaseConstructor` expect/actual (Room generates).
- `DatabaseModule` with DAO providers.

`:data-services:backend`:

- `BackendClient`, `TokenProvider`, `ClientLogger`, `<Product>Api`.
- Minimal DTOs: `TokenResponse`, `RefreshBody`, plus the first product's response (e.g. `TaskResponse`).
- `BackendModule`.

`:data-services:firebase` (if enabled):

- `FirebaseProvider` interface with `Analytics`, `Crashlytics`, `Messaging` sub-interfaces.
- Android implementation (uses Firebase BOM libs).
- iOS empty stub.

## Step 7.5 — implement the `:ui-dialog-features:dialog-api` contract module

Step 3 scaffolded the module as an empty `.gitkeep`. Fill in its five files now — `:shared`'s `DialogComponent` (Step 9) and the error pipeline (the `ui-feature` skill's error-pipeline reference) both depend on these contracts existing.

Authoritative reading: the `ui-feature` skill's dialog-navigation reference, plus its ui-feature-modules reference § "`:ui-dialog-features:dialog-api`".

Inside `src/commonMain/kotlin/com/<org>/<product>/dialog/api/`:

- `Constants.kt` — `public val DIALOG_EXIT_ANIMATION_DURATION: Duration = 300.milliseconds`. The shared `DialogScreen` uses this when scheduling pending callbacks against the sheet's hide animation.
- `DialogConfig.kt` — `@Serializable public sealed class DialogConfig(@Transient public open val onDismiss: (() -> Unit)? = null, public open val dismissBySwipe: Boolean = true) { public abstract val key: String; protected fun buildKey(vararg parts: Any?): String = /* length-prefixed join, see the `ui-feature` skill's dialog-navigation reference */ }`. Seed exactly **one** subtype — `ErrorDisplay` — because the error pipeline (the `launch-readiness` skill's architecture-overview reference) references it:

  ```kotlin
  @Serializable
  public data class ErrorDisplay(
      val error: AppErrorState,   // from :ui-core:state (seeded in Step 4.5)
      @Transient val onClose: () -> Unit = {},
  ) : DialogConfig(
      onDismiss = onClose,
      dismissBySwipe = true,
  ) {
      override val key: String get() = buildKey("ErrorDisplay", error)
  }
  ```

  `AppErrorState` is imported from `:ui-core:state` (`com.<org>.<product>.core.state.error.AppErrorState`, seeded in Step 4.5). The dialog-api `build.gradle.kts` template in the `ui-feature` skill's ui-feature-modules reference already lists `implementation(projects.uiCore.state)`; no additional dep needed.

  Further subtypes (`Confirmation`, pickers, etc.) are added per the add-dialog cookbook recipe in the `ui-feature` skill as each dialog feature lands. Do **not** seed more here.
- `DialogController.kt` — two interfaces in one file:

  ```kotlin
  public interface DialogController {
      public fun show(config: DialogConfig)
  }

  public interface DialogProvider {
      public val dialog: Flow<DialogConfig>
  }
  ```

  `DialogController` has **only** `show(config)`. There is no `dismiss()` method — closing the sheet is the host's job via `DialogContentComponent` → `DialogViewModel.onDismiss(...)`. Do not invent one.
- `DialogModule.kt` — `@Module @ComponentScan public class DialogModule`. This is the Koin module for the controller; **it lives here, not in `:shared`**. `:shared/Koin.kt` lists it alongside the other feature modules.
- `internal/DialogControllerImpl.kt` — `@Single(binds = [DialogController::class, DialogProvider::class]) internal class DialogControllerImpl : DialogController, DialogProvider`. Uses a `Channel<DialogConfig>(Channel.BUFFERED)`; `show(...)` calls `_dialog.trySend(config)`; `dialog: Flow<DialogConfig> = _dialog.receiveAsFlow()`. Internal — only Koin instantiates it.

Verify: `./gradlew :ui-dialog-features:dialog-api:assemble` builds green.

## Step 7.6 — implement `:ui-core:error:error-provider-impl`

Step 3 scaffolded the module as an empty `.gitkeep`. Fill it in now — without this module, `BaseViewModel.safeLaunch { ... }`'s `inject<ErrorProvider>()` resolves to nothing and the first failing coroutine crashes with a Koin `NoDefinitionFoundException` at runtime. The module also closes the loop between the error contract (`:ui-core:error:error-provider`, Step 4.5) and the dialog pipeline (`:ui-dialog-features:dialog-api`, Step 7.5): `ErrorProviderImpl` maps `AppError` → `AppErrorState` and calls `DialogController.show(DialogConfig.ErrorDisplay(...))`.

Authoritative reading: the `ui-feature` skill's ui-core-modules reference § "`:ui-core:error:error-provider-impl`", plus its error-pipeline reference.

### `build.gradle.kts`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.<org>.<product>.ui.core.error.error.provider.impl"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.uiCore.state)
    }
}
```

`projects.uiDialogFeatures.dialogApi` pulls in `DialogController` + `DialogConfig.ErrorDisplay` (the route this module dispatches to); `projects.uiCore.error.errorProvider` pulls in the `ErrorProvider` interface + `AppError` hierarchy this module implements; `projects.uiCore.state` provides the `AppErrorState` subtypes the mapper constructs. The same three deps are listed in the `ui-feature` skill's ui-core-modules reference § "`:ui-core:error:error-provider-impl`".

Inside `src/commonMain/kotlin/com/<org>/<product>/core/error/provider/impl/`:

- `ErrorModule.kt` — `@Module(includes = [DialogModule::class]) @ComponentScan public class ErrorModule`. Listed in `:shared/Koin.kt` (Step 9) so the `@Single` on `ErrorProviderImpl` is discovered; the `includes = [DialogModule::class]` saves `:shared/Koin.kt` from having to declare both modules explicitly — depending on `ErrorModule` automatically pulls `DialogModule`.
- `ErrorProviderImpl.kt` — `@Single(binds = [ErrorProvider::class]) internal class ErrorProviderImpl(val dialogController: DialogController) : ErrorProvider`. Implements `override suspend fun provide(exception: Throwable, callback: () -> Unit)` as an exhaustive `when` over `AppError` subtypes that produces an `AppErrorState`, then calls `dialogController.show(DialogConfig.ErrorDisplay(error = state, onClose = callback))`. Internal — only Koin instantiates it. Branches mirror the `ui-feature` skill's error-pipeline reference verbatim:

  ```kotlin
  val error = when (exception) {

      is AppError.Network.NoInternet -> AppErrorState.Network.NoInternet(
          description = exception.message,
      )

      is AppError.Network.Timeout -> AppErrorState.Network.Timeout(
          description = exception.message,
      )

      is AppError.Network.Expected -> AppErrorState.Network.Expected(
          title = exception.title,
          description = exception.description,
      )

      is AppError.Network.Unexpected -> AppErrorState.Network.Unexpected(
          description = exception.message,
      )

      is AppError.Expected -> AppErrorState.Expected(
          title = exception.message,
          description = exception.description,
      )

      is AppError.Unknown -> AppErrorState.Unknown

      else -> AppErrorState.Unknown
  }
  ```

  The trailing `else -> AppErrorState.Unknown` is intentional — `BackendClient.HttpResponseValidator` may surface a `Throwable` outside the `AppError` hierarchy (a bug, an unmapped library exception); the pipeline still produces a visible dialog rather than swallowing it.

## Step 7.7 — implement `:ui-dialog-features:error-display`

The error pipeline's render target — **not** an optional feature dialog. `ErrorProviderImpl` (Step 7.6) maps every `Throwable` to `DialogConfig.ErrorDisplay` and calls `dialogController.show(...)`; `:shared`'s `DialogContentComponent.createChild` (Step 9) builds this Component for it. Without this module, Step 9's branch is forced to a `TODO(...)`/stub and the **first** runtime error of any kind crashes the app with `NotImplementedError` instead of showing the error sheet. Scaffold it here — after the error contracts (Step 4.5) and `dialog-api` (Step 7.5), before `:shared` (Step 9).

Authoritative reading: the add-dialog cookbook recipe in the `ui-feature` skill, plus its error-pipeline reference. Package/token paths below mirror Step 4 (`core.foundation`), Step 4.5 (`core.state.error`), and Step 6 (`design.*`) — adjust to your project's actual subpackages.

### 1. `settings.gradle.kts`

```kotlin
include(":ui-dialog-features:error-display")
```

### 2. `build.gradle.kts`

Same plugin set + deps as the cookbook dialog template (add-dialog recipe in the `ui-feature` skill, step 2):

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.dialog.features.error.display" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

### 3. The seven MVI files

Under `src/commonMain/kotlin/com/<org>/<product>/error/display/` (imports mirror the cookbook dialog; `AppErrorState` is from `:ui-core:state`, `core.state.error`):

```kotlin
// ErrorDisplayState.kt
@Immutable
public data class ErrorDisplayState(val error: AppErrorState)

// ErrorDisplayDirection.kt
public sealed interface ErrorDisplayDirection : BaseDirection {
    public data object Back : ErrorDisplayDirection
}

// ErrorDisplayLoader.kt
@Immutable
public sealed interface ErrorDisplayLoader : BaseLoader

// ErrorDisplayContract.kt
@Immutable
internal interface ErrorDisplayContract {
    fun onDismiss()
    @Immutable companion object Empty : ErrorDisplayContract { override fun onDismiss() = Unit }
}

// ErrorDisplayViewModel.kt
public class ErrorDisplayViewModel(
    error: AppErrorState,
) : BaseViewModel<ErrorDisplayState, ErrorDisplayDirection, ErrorDisplayLoader>(
    ErrorDisplayState(error = error),
), ErrorDisplayContract {
    override fun onDismiss() = navigateTo(ErrorDisplayDirection.Back)
}

// ErrorDisplayComponent.kt — public: :shared constructs it across the module boundary
public class ErrorDisplayComponent(
    componentContext: ComponentContext,
    private val error: AppErrorState,
    private val back: () -> Unit,
) : BaseComponent<ErrorDisplayDirection>(componentContext) {

    override val viewModel: ErrorDisplayViewModel =
        componentContext.retainedInstance { ErrorDisplayViewModel(error = error) }

    init { backHandler.register(BackCallback(onBack = viewModel::onDismiss)) }

    override suspend fun eventListener(direction: ErrorDisplayDirection) {
        when (direction) { ErrorDisplayDirection.Back -> back.invoke() }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ErrorDisplayScreen(state.value, loaders.value, viewModel)
    }
}
```

`ErrorDisplayScreen.kt` — the cookbook-02 dialog shell (`BaseComposeScreen` + `Spacer(AppTokens.dp.dialog.top)` … `Spacer(AppTokens.dp.dialog.bottom)` + `navigationBarsPadding()`), rendering the error's title + optional description + a dismiss button:

```kotlin
@Composable
internal fun ErrorDisplayScreen(
    state: ErrorDisplayState,
    loaders: ImmutableSet<ErrorDisplayLoader>,
    contract: ErrorDisplayContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding).fillMaxWidth(),
        text = state.error.title(),                  // plain-String accessor — step 4 below
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center,
    )

    state.error.description()?.let { description ->
        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))
        Text(
            modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding).fillMaxWidth(),
            text = description,
            style = AppTokens.typography.b14Med(),    // any body token your design-system exposes
            color = AppTokens.colors.text.secondary,  // or text.primary if the palette has no secondary
            textAlign = TextAlign.Center,
        )
    }

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding).fillMaxWidth(),
        content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.error_dismiss)),
        style = ButtonStyle.Primary,
        onClick = contract::onDismiss,
    )

    Spacer(Modifier.size(AppTokens.dp.dialog.bottom))
    Spacer(Modifier.navigationBarsPadding())
}
```

Add an `@AppPreview` preview from `ErrorDisplayState(AppErrorState.Expected(title = "Something went wrong", description = "Could not reach the server."))` + `ErrorDisplayContract.Empty`, per the cookbook.

### 4. Plain-String accessors in `:ui-core:state`

The bootstrap `AppErrorState` intentionally carries plain strings, while the full UI layer may later adopt `UiText` for localized resources. Its `Network.{NoInternet,Timeout,Unexpected}` / `Unknown` subtypes have no `title`. Add `AppErrorStateExt.kt` next to `AppErrorState` (`core.state.error`) so the Screen reads a uniform title/description:

```kotlin
// Bootstrap shape: plain-String labels. Swap to UiText + Res.string when the
// product adds localization (add-resource cookbook recipe in the design-system skill).
public fun AppErrorState.title(): String = when (this) {
    is AppErrorState.Network.NoInternet -> "No internet connection"
    is AppErrorState.Network.Timeout    -> "Request timed out"
    is AppErrorState.Network.Expected   -> title
    is AppErrorState.Network.Unexpected -> "Something went wrong"
    is AppErrorState.Expected           -> title
    AppErrorState.Unknown               -> "Something went wrong"
}

public fun AppErrorState.description(): String? = when (this) {
    is AppErrorState.Network.NoInternet -> description
    is AppErrorState.Network.Timeout    -> description
    is AppErrorState.Network.Expected   -> description
    is AppErrorState.Network.Unexpected -> description
    is AppErrorState.Expected           -> description
    AppErrorState.Unknown               -> null
}
```

### 5. The `error_dismiss` string

Add `<string name="error_dismiss">Got it</string>` to `:design-system:resources:provider`'s `values/strings.xml` and every `values-<lang>/strings.xml` for the project's `supportedLocales` (add-resource cookbook recipe in the `design-system` skill).

### 6. Verify

```bash
./gradlew :ui-core:state:assemble :ui-dialog-features:error-display:assemble
```

Builds green. Step 9 then wires `ErrorDisplayComponent` into `DialogContentComponent.createChild` and adds `implementation(projects.uiDialogFeatures.errorDisplay)` to `:shared/build.gradle.kts`.

## Step 8 — implement the first feature

> This step is the launch-time equivalent of the feature-building skill rules. Keep the two shapes in sync when either changes.

Use the first product domain the user specified in Step 0 question 5 as the first feature. Build the simplest end-to-end slice (e.g. a list screen for that domain).

1. **Domain types** in `:data-features:feature-api`:
   - `<X>Feature` interface.
   - `<X>` domain model.
2. **Implementation module** `:data-features:<x>` (add to `settings.gradle.kts`):
   - `<X>Repository` interface.
   - `<X>RepositoryImpl`.
   - `<X>FeatureImpl`.
   - `<X>FeatureModule`.
3. **Mappers** in `:data-mappers:*`:
   - `<X>Response.toEntityOrNull()`, `List<>.toEntities()`.
   - `<X>Entity.toDomain()`, `List<>.toDomain()`.
   - `<X>.toState()`, `List<>.toState()`.
4. **Screen feature** `:ui-screen-features:<x>` (add to `settings.gradle.kts`):
   - `RootRouter` sealed class in `:screen-api` (the top-level `BaseRouter` config; see the `ui-feature` skill's shared-composition-root and decompose-navigation references for shape) — one entry per top-level feature, initially `<X>`.
   - `<X>Router` in `:screen-api`.
   - Seven MVI files for the list screen.
   - `<X>RootComponent`.

## Step 9 — implement `:shared`

`:shared` is the composition root. Create:

- `Koin.kt` listing every `<X>Module` so far — including `DialogModule().module` (ships in `:ui-dialog-features:dialog-api`, set up in Step 7.5) and `ErrorModule().module` (ships in `:ui-core:error:error-provider-impl`, set up in Step 7.6). Neither is re-declared in `:shared`.
- `RootComponent`, `RootViewModel`, `RootScreen`, `RootDirection`, `RootContract`, `RootState`, `RootLoader` (under `root/`).
- The outer dialog navigator under `dialog/` — exactly **seven** files matching the seven-file MVI shape:
  - `DialogComponent.kt` — owns `SlotNavigation<DialogConfig>`, builds `DialogContentComponent` as the slot child, runs a reconciliation loop against `DialogViewModel.state` (activate/dismiss the slot to match `sessionConfig`; push/pop the inner stack to match `innerConfigs`).
  - `DialogContract.kt` — `internal interface DialogContract { fun onClose(); fun onDismiss(pendingResult: (() -> Unit)?); fun onRelease(config: DialogConfig); companion object Empty : DialogContract { ... } }`.
  - `DialogState.kt` — `internal data class DialogState(val stack: ImmutableList<DialogEntry> = persistentListOf(), val phase: SheetPhase = SheetPhase.Released, val pending: DialogConfig? = null)` with derived `sessionConfig` / `innerConfigs`; plus `DialogEntry(config, pendingResult)` and `sealed class SheetPhase { Present, Dismissing, Released }`.
  - `DialogDirection.kt` — `internal sealed interface DialogDirection : BaseDirection` (no cases — reconciliation is state-driven).
  - `DialogLoader.kt` — `internal sealed interface DialogLoader : BaseLoader`.
  - `DialogViewModel.kt` — consumes `DialogProvider.dialog`, manages the stack/phase, deduplicates by `config.matches(other)` (same class and same `key`), routes `onClose` / `onDismiss(pendingResult)` / `onRelease(config)`. Pending callbacks fire **before** clearing the stack so chained `show(...)` calls land as `pending` and are promoted on the next release tick.
  - `DialogScreen.kt` — renders `ModalBottomSheet` with `rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { ... })`; uses `BottomSheetToolbar` (back when `stack.size > 1`, close otherwise); reads `dismissBySwipe` via `rememberUpdatedState` so the latest value wins on recomposition; coordinates `sheetState.hide()` / `sheetState.show()` with `SheetPhase.Dismissing` / `SheetPhase.Present`.
- The inner dialog stack under `dialog/content/` — another seven files:
  - `DialogContentComponent.kt` — owns `StackNavigation<DialogConfig>` (`initialStack = { listOf(initial) }`); its `createChild(router, context)` is the exhaustive `when` over `DialogConfig` sub-types. Declare a private `internal sealed class Child(open val component: BaseComponent<*>)` inside the component — the per-feature data classes (one per `DialogConfig` subtype) live on it. After Step 7.5/7.7 the only seeded subtype is `ErrorDisplay`, wired to the `ErrorDisplayComponent` scaffolded in Step 7.7:

    ```kotlin
    private fun createChild(router: DialogConfig, context: ComponentContext): Child = when (router) {
        is DialogConfig.ErrorDisplay -> Child.ErrorDisplay(
            ErrorDisplayComponent(
                componentContext = context,
                error = router.error,
                back = { viewModel.onBack(null) },
            )
        )
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class ErrorDisplay(override val component: ErrorDisplayComponent) : Child(component)
    }
    ```

    `ErrorDisplay` is **not** a deferrable feature dialog — it is the render target of the error pipeline (the `ui-feature` skill's error-pipeline reference): every `safeLaunch` failure funnels through `ErrorProvider.provide` → `DialogController.show(DialogConfig.ErrorDisplay(...))` → here. A `TODO(...)`/stub in this branch turns the **first** runtime error of any kind into a `NotImplementedError` crash instead of an error sheet — so Step 7.7 scaffolds `:ui-dialog-features:error-display` as foundation, before this wiring, and `:shared/build.gradle.kts` must list `implementation(projects.uiDialogFeatures.errorDisplay)`. New dialog features added later (add-dialog cookbook recipe in the `ui-feature` skill, step 5) add a `is DialogConfig.<Name> -> Child.<Name>(...)` branch and a matching `data class <Name>(override val component: <Name>Component) : Child(component)` per the standard pattern.
  - `DialogContentContract.kt` — `internal interface DialogContentContract { fun onBack(pendingResult: (() -> Unit)?); companion object Empty }`.
  - `DialogContentState.kt` — `internal data object DialogContentState`.
  - `DialogContentDirection.kt` — `internal sealed interface DialogContentDirection : BaseDirection { data class Back(val pendingResult: (() -> Unit)? = null) : DialogContentDirection }`.
  - `DialogContentLoader.kt` — `internal sealed interface DialogContentLoader : BaseLoader`.
  - `DialogContentViewModel.kt` — translates `onBack(pendingResult)` into `navigateTo(DialogContentDirection.Back(pendingResult))`. `DialogContentComponent.eventListener` then hands the `pendingResult` lambda upward to the host (the outer `DialogViewModel.onDismiss`).
  - `DialogContentScreen.kt` — animates between inner-stack children with `AnimatedContent` keyed on `child.keyHashString()`, retains saveable state via `rememberSaveableStateHolder()` + cleanup of obsolete keys on stack change.
- `Deeplink` enum (empty initially or with one entry) — lives in `:ui-screen-features:screen-api`, not `:shared`. `:shared/RootViewModel` only references it via `Deeplink.fromKey(raw)`.
- iOS bridge `RootViewController.kt` in `iosMain` — `public fun rootViewController(root: RootComponent, backDispatcher: BackDispatcher): UIViewController` (wraps `root.Render()` in a `ComposeUIViewController` + `PredictiveBackGestureOverlay`). Swift calls it as `RootViewControllerKt.rootViewController(root:backDispatcher:)`. See the `platform-build-toolkit` skill's app-shells reference § "`iosMain/RootViewController.kt`".

`RootComponent` instantiates `DialogComponent` once and renders it as a sibling of `RootScreen` inside the `AppTheme { ... }` block (not nested inside `RootScreen`); see the `ui-feature` skill's shared-composition-root reference § "`RootComponent` shape" for the exact `Render()` body.

Verify: `./gradlew :shared:assemble<IosFrameworkName>DebugXCFramework`. With the default `iosFrameworkName: shared`, the task is `./gradlew :shared:assembleSharedDebugXCFramework`. The module path is always `:shared:`; only the `<IosFrameworkName>` task-name suffix tracks the project's framework name.

## Step 10 — implement `:androidApp`

Create:

- `App.kt` — `Application` subclass with `Koin.init { androidContext(this); androidLogger() }` and (if Firebase) `FirebaseProvider.setup(this)`.
- `MainActivity.kt` — single Activity with `retainedComponent { RootComponent(it, close = ::finishAffinity, deeplink = intent.getStringExtra(LocalNotificationExtras.DEEPLINK)) }` and `setContent { root.Render() }`. The `deeplink` argument is a raw `String?` lifted from the launch intent's extras — the `String`→`Deeplink` parse already lives in `RootViewModel.parseDeeplink(raw)` (see the `platform-build-toolkit` skill's app-shells reference § Android shell), so do **not** add a `parseDeeplink` helper in the Activity.
- `AndroidManifest.xml` — `<application android:name=".App">`, `<activity android:name=".MainActivity" android:exported="true">` with the main launcher intent filter and deeplink filters if any.
- `build.gradle.kts` — verbatim from the `platform-build-toolkit` skill's android-app-module reference.
- `proguard-rules.pro` — empty for now (R8 defaults).
- `google-services.json` — placeholder; user will replace. *(Firebase only — omit if `firebaseEnabled: false`.)*

Verify: `./gradlew :androidApp:assembleDebug`.

## Step 11 — implement `:iosApp`

`:iosApp` is an Xcode project (not a Gradle module) that links the static XCFramework from `:shared`. Build it from the verbatim drop-in fences in the `platform-build-toolkit` skill's ios-app-project reference: copy each file to its `Target path` and substitute placeholders, exactly like the base-class reference implementations in Step 4.

Apply the `platform-build-toolkit` skill's ios-app-project reference:

1. Create every file from its fence at the listed `Target path`. Substitute `<Product>`, `<iosFrameworkName>`, `<org>` from `orchestrator/project-config.md`; `<bundleId>` = the iOS bundle id (same as `applicationId` from `orchestrator/project-config.md` — no platform suffix); `<TEAM_ID>` = your Apple Developer Team ID (empty is fine for the simulator; set it for device builds). `<bundleId>` and `<TEAM_ID>` are documented in the placeholder legend at the top of the ios-app-project reference — they are not keys in `project-config.md`.
2. **Firebase gate** (`firebaseEnabled`): if `true`, use the `project.pbxproj (firebaseEnabled: true)` fence, keep `IosFirebase{Analytics,Crashlytics,Messaging}.swift` + the placeholder `GoogleService-Info.plist`, and keep the `// region firebase-conditional` blocks in `AppDelegate.swift`. If `false`, use the `project.pbxproj (firebaseEnabled: false)` fence, omit those files, and strip everything between the `// region firebase-conditional` / `// endregion firebase-conditional` markers.
3. `chmod +x iosApp/run-ios.sh`.
4. Build the framework once so Xcode can resolve `import <iosFrameworkName>`:
   `./gradlew :shared:assemble<IosFrameworkName>DebugXCFramework` (default: `:shared:assembleSharedDebugXCFramework`).
5. (Firebase) Replace the placeholder `iosApp/GoogleService-Info.plist` with the real file from the Firebase console. The app builds with the stub; Firebase just won't connect until it is replaced.

The `project.pbxproj` ships a `Compile Kotlin Framework` run-script (`./gradlew :shared:embedAndSignAppleFrameworkForXcode`) + `FRAMEWORK_SEARCH_PATHS`, so the framework rebuilds on every Xcode build — **no drag-and-drop**. The **shared scheme** makes the `iosApp` run configuration appear in Android Studio's run-target dropdown (KMP plugin) and lets `xcodebuild -scheme iosApp` resolve in CI.

The drop-in set intentionally omits product-specific pieces (push/APNs wiring, notification deeplinks, Google Sign-In `Info.plist` keys, entitlements, real app icons) — add them when the product needs them. The *why* behind the Xcode-side wiring is in the `platform-build-toolkit` skill's ios-swiftpackage reference.

## Step 12 — verify end-to-end

Run (substitute the `<IosFrameworkName>` task-name suffix per the project-config value; defaults to `Shared`. The module path stays `:shared:`):

```bash
./gradlew :shared:assemble<IosFrameworkName>DebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both must succeed.

**Foundation integrity gate.** A green `assemble` does **not** prove the app survives its first error — `TODO(...)` returns `Nothing`, so a stubbed load-bearing branch (the `DialogConfig.ErrorDisplay` → `createChild` route is the canonical trap) compiles and ships, then crashes with `NotImplementedError` the moment any error fires. Fail the bootstrap if a stub survives in foundation source:

```bash
node orchestrator/site/server/foundation-stub-scan.js
```

Exit 0 = clean; on hits it prints `file:line` per stub and exits 1. The scan is comment/string-aware: a comment or string literal that merely *mentions* `TODO(...)`/`NotImplementedError` does not fail the gate — only real code-position stubs do. This is the same implementation behind the site's Step-12 ✓ validator, so the moment this command passes, the wizard marks Step 12 done on its own — do not mark it manually and do not re-derive the scan with a raw `rg` (raw greps false-positive on comments).

Run the Android app: a screen renders with the toolbar and at least one element from the design system.

Run the iOS app — open `iosApp/iosApp.xcodeproj` in Xcode and Run, or pick the `iosApp` configuration in Android Studio's run dropdown (the shared scheme surfaces it), or run headless with `./iosApp/run-ios.sh`. The same screen renders.

If both succeed, the foundation is complete.

Fixes required to make Step 12 pass — a missing `INTERNET` permission, a broken run script, a surviving stub — are part of the bootstrap: apply them, re-run the failed check, and list them in the step report. Leave them in the working tree with the rest of the bootstrap output; do not ask whether to apply, commit, or keep them, and do not end the step with a question. Findings outside the foundation (product/backend behavior, cosmetic UI polish) go into the report as notes, not questions.

## Step 13 — write `CLAUDE.md`

**Precondition.** Check the project root for an existing `CLAUDE.md`. If one is
present, never delete or rewrite it and do not ask about it: keep the existing
content verbatim and append only the sections below that are missing, under a
clearly-marked `## Orchestrator workflow` heading, then note the pre-existing file
in the step report so the owner can reconcile later. There must be one coherent
instruction file at this scope.

```bash
# Inspect first — never rm an existing CLAUDE.md; append missing sections instead:
[ -f CLAUDE.md ] && head -40 CLAUDE.md
```

Do not interview the user for content. Derive the product-purpose paragraph from the Step 0 answers (product name + first product domain); write "(filled in later)" for anything the Step 0 answers do not cover (positioning, cross-repo rules) — do not invent content.

At the project root, create `CLAUDE.md` describing:

- The product purpose (one paragraph — derived from the Step 0 answers).
- Cross-repo rules ("(filled in later)" unless the Step 0 answers cover them).
- Communication / decision-making conventions.
- Scope discipline ("don't refactor without an explicit request", etc.).
- When to stop and ask (mirror the `validation-gates` skill's when-to-stop-and-ask reference).
- Task workflow pointer: "Tasks flow through the four-column kanban under `orchestrator/tasks/`: `backlog -> task-prep -> pending -> task-prep -> todo -> orchestrator -> done`. Drop free-text ideas in `backlog/`, run `task-prep`; if questions land in `pending/`, answer them and run `task-prep` again; then ask Claude to run the task once it is in `todo/`. The orchestrator moves the file to `done/` on success. See `orchestrator/tasks/README.md`."
- **(only if `figmaEnabled: true`)** A short **"Figma implementation rules"** block: *Figma provides design context; production Kotlin components + `AppTokens.*` are the source of truth. Never copy React/Tailwind/HTML/CSS output literally. Always consult the component mapping registry (`orchestrator/figma/component-mappings.json` + the Design → Components panel) and the `implement-figma` skill before building from a Figma node. Never recreate a registered component with `Box`/`Row`/`Column`/`Surface`/`Canvas`. Never hardcode a design value when an `AppTokens.*` equivalent exists. Report missing mappings instead of silently approximating. Use localization resources for user-facing text.* (Mirrors the `implement-figma` skill's overview; omit this block entirely when `figmaEnabled: false`.)
- **(only if `backendContractEnabled` is not `false`)** A short **"Backend contract rules"** block: *The validated current generation (resolve exact inventory and area paths with `npm run --silent contract:paths` in `orchestrator/api-contract/`) is the contract of record for endpoint paths, methods, field names, types, nullability, and enums. Consult it before writing or changing any DTO / `<apiClassName>` method / mapper; never invent endpoints or fields. The all-nullable defensive DTO discipline stays regardless of what the spec declares. When the backend changes, refresh the snapshot in the Backend tab or with typed `contract:probe` then the matching `contract:refresh-*`, and act on `backend-contract-drift` findings instead of patching blind.* (Mirrors the `backend-contract-client` skill's overview; omit this block entirely when `backendContractEnabled: false`.)

This file becomes the high-level guide for future agent/contributor work. The detailed skills under `orchestrator/skills/` (deployed to `.claude/skills/`) are the source of truth.

## Step 14 — install skills

After the foundation builds green and the first end-to-end "hello world" feature is in place, install the skill toolkit so ongoing work can be automated. (If you bootstrapped through the site's **Setup** panel, the toolkit was already installed there — in that flow this step only verifies.)

Install the 11 skills plus the frozen contracts, queue command, and launch
configuration with the deploy script (run from the project root):

```bash
bash orchestrator/skills/install-skills.sh .          # copy; add --symlink for a dev checkout
ls .claude/skills/   # expect at least 11 skill directories; 12 when implement-figma was installed in Step 6.5
bash orchestrator/skills/checks/install-sync.sh .     # exact skill/reference/contract/command/launch parity
cmp orchestrator/skills/_index/install-surfaces/commands/serve-queue.md .claude/commands/serve-queue.md
cmp orchestrator/skills/_index/install-surfaces/launch.json .claude/launch.json
# Stamp the vendored-copy integrity manifest — the drift sensor's baseline
# (orchestrator/template-sync/README.md). Stamped ONLY here and by an explicit
# sync-from-template.sh --apply; never by a hook or daemon (an auto-regen would
# re-baseline local drift as "clean" and the sensor could never report rot).
python3 orchestrator/template-sync/_generate_template_manifest.py
# Seed the derived architecture map BEFORE linting: lint.sh check 4 requires
# orchestrator/.arch-map.json once a product project exists (settings.gradle.kts is present
# after Steps 2–11). It is the token-saving structural hint the orchestrator's Step 1a and
# context-finder read; every shipped task then refreshes it via Step 6c.
python3 orchestrator/tasks/regen-arch.py
bash orchestrator/lint.sh
```

Run `regen-arch.py` and `lint.sh` back-to-back, with no intervening module or `settings.gradle.kts` edit. lint check 4 (`--check`) compares the committed `.arch-map.json` against `settings.gradle.kts`; a concurrent auto-commit/runner editing settings between the regen and the check makes check 4 go spuriously STALE. A STALE check 4 immediately after a clean regen means re-run regen — it is not a real defect.

**Enforcement-wiring verification (only when `figmaEnabled: true`).** `install-skills.sh` just
wired `core.hooksPath` (the pre-commit `verify-done` net). Confirm it took — a figmaEnabled
product without that net can ship an uncompared UI task via a bare `git mv` (the "vendored
product, zero enforcement" failure). Run the strict doctor check:

```bash
FIGMA_STRICT_WIRING=1 npm --prefix orchestrator/figma run figma:doctor
```

It FAILs (non-zero) if `core.hooksPath` is not `orchestrator/skills/checks/hooks`. If it fails,
re-run `bash orchestrator/skills/install-skills.sh .`. Do this before shipping any UI task.
(A plain `figma:doctor` reports the same as an advisory `WARN`; strict makes it a hard gate
here, post-install.)

Verify `orchestrator/project-config.md` was populated in Step 1.5. If you skipped Step 1.5 for any reason, do it now before invoking any skill — they will all fail with BLOCKED otherwise.

Make sure the task-board folders exist (they ship empty in `orchestrator/tasks/` and need to be on disk before `task-prep` or the orchestrator runs):

```bash
mkdir -p orchestrator/tasks/{backlog,pending,todo,done}
[ -f orchestrator/tasks/INDEX.json ] || cat > orchestrator/tasks/INDEX.json <<'EOF'
{
  "version": 2,
  "generatedAt": "1970-01-01T00:00:00.000Z",
  "backlog": [],
  "pending": [],
  "todo": [],
  "done": []
}
EOF
```

The sentinel `generatedAt` is replaced with a real timestamp the first time `task-prep` or `orchestrator` regenerates the file.

Tasks now flow per `orchestrator/tasks/README.md` § Lifecycle.

### The pre-commit gate

The per-task run loop enforces the mandatory Figma↔app comparison while a task is running; the
local hook is what stops an uncompared task from being committed afterwards. The template's
`orchestrator/skills/checks/hooks/pre-commit` therefore ALSO runs
`node orchestrator/figma/scripts/verify-done.mjs` and **blocks the commit** on any `done/` UI task
whose Figma-meta digest is missing or non-shippable (BLOCKER/INCOMPLETE) — forcing the run loop to
finish the captures/qualifiers to PASS/WARN rather than bare-`mv` past a red gate. It is a no-op
when `figmaEnabled: false` (0 audited) and the deliberate emergency bypass is `git commit
--no-verify`.

**REQUIRED install step (do this at bootstrap — the hook is inert until wired):** point git at the
tracked hooks dir so the source hook IS the live hook (no copy, no drift):

```bash
git config core.hooksPath orchestrator/skills/checks/hooks
```

`orchestrator/skills/install-skills.sh` runs this automatically when the target is already a git
repo (it prints an action-required line otherwise), so this manual step doubles as verification.

Run this once when the product repo is created. Until it is run, the hook file exists but git never
executes it. An unwired figmaEnabled product cannot run tasks: the site runner
refuses `run` sessions (`sessions.runGateError()`, server/sessions.js — the request is 409'd at
enqueue, queued ones are held un-claimed, and the header Skills pill shows the wiring command), and
the run-loop's Step 0 bootstrap check emits `BLOCKED[figma-wiring]` for site-less runs (a /loop
worker or a manual terminal session). Verify with a throwaway commit that leaves a red `done/` UI task: it must be
rejected.

## Constraints

- **Never ask after Step 0.** The run is unattended: no clarifying questions, no confirmation requests, no closing "Want me to…?" offers. Resolve by the documented defaults and record the decision in the step report; a missing required input ends the run with `BLOCKED: <what is missing>`.
- **Build green at every step.** If a step's verification fails, fix the issue before moving on. Don't accumulate broken modules.
- **Use the verbatim code from the skills** (each skill's `references/`) as the starting point — substitute the `<org>`, `<product>`, and `<Product>` placeholders consistently with the values gathered in Step 0. Don't invent new code.
- **Follow the seven-file MVI pattern** for every screen and dialog. No shortcuts.
- **One feature in step 8.** Don't try to scaffold multiple features in this bootstrap pass.
- **Every task leaves executable proof.** New or changed observable behavior gets a behavioral test on its proven lane; bugfixes get a regression test; wiring/resources/scaffold get structural gates. The machine authority is `orchestrator/tasks/test-policy.json` (rationale: `orchestrator/contracts/test-policy.md`); the bootstrap itself only ships the opt-in foundation (Step 2) — certification runs per task, not during bootstrap.
- **Don't add dependencies** beyond what `gradle/libs.versions.toml` declares.
- **Don't modify the skills under `orchestrator/skills/`** during bootstrap — they're the contract.
- **Don't skip the toolkit modules** — they're "trivial" but required by the convention plugins and feature code.

## Iteration model

Bootstrap takes 1–3 hours of agent work for the foundation. Run incrementally:

1. After Step 2 (build system) — verify Gradle works.
2. After Step 5 (toolkit) — verify each module builds.
3. After Step 7 (data layer) — verify Database and BackendClient compile.
4. After Step 9 (`:shared`) — verify XCFramework builds.
5. After Step 12 (end-to-end verify) — verify both platforms launch. (Step 11 generates the iOS Xcode project from templates — no manual Xcode wiring; the cross-platform XCFramework + androidApp assemble checks live in Step 12.)

Don't push to a remote branch until Step 12 passes.

## When you're stuck

If a step fails:

- Re-read the relevant skill (its `SKILL.md` + the matching `references/` file).
- Check the `validation-gates` skill's forbidden-patterns reference — you may be doing something the architecture forbids.
- Check the owning skill's cookbook recipes — there's likely a similar recipe.
- If the failure is in convention plugins, re-read the `platform-build-toolkit` skill's convention-plugins reference and compare verbatim.

Don't improvise. The architecture has reasons for every choice.
````

---

## Outcome

After the agent completes `launch.md`:

- The project has the full mandatory infrastructure (~38 modules).
- One end-to-end feature is wired (`:data-features:<x>`, `:data-mappers:*`, `:ui-screen-features:<x>`).
- Android and iOS both launch and render the feature.
- `CLAUDE.md` exists for future agent context.
- The team can grow from here using the per-skill cookbook recipes (carried by each owning skill).

## What this is NOT

- **It is NOT a one-shot perfect bootstrap.** Expect 1–2 follow-up rounds to fix initial bugs (icon assets, Firebase config, signing keys).
- **It is NOT a code generator.** The agent writes the code; the user reviews and adjusts.
- **It is NOT product-aware.** The user picks the first feature; the agent implements the chosen one.
