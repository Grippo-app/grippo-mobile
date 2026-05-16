# Launch — Bootstrap a New Project from These Requirements

This prompt is fed to an agent (Claude / GPT / etc.) at the **root of an empty directory** to scaffold a new KMP mobile project that conforms to the `requirements/` set. The agent works **iteratively**, building one layer at a time, verifying each layer compiles before moving on.

Run the prompt **once** at the start of a new project. Re-running it later is unnecessary — the cookbook (`requirements/14-cookbook/`) covers ongoing growth.

---

## How to use

1. Copy the `requirements/` folder to the new project's root.
2. Feed the prompt below to your agent.
3. Answer the agent's clarifying questions about product name, org, package, etc.
4. The agent will create modules incrementally and verify each layer with `./gradlew` builds.
5. After the agent completes the foundation, follow the cookbook to add features.

---

## The prompt

```
You are bootstrapping a new Kotlin Multiplatform (KMP) mobile project from scratch. The architecture is fully specified in the `requirements/` folder at the project root. Read EVERY file in `requirements/` before starting — they are normative.

Goal: produce a project that builds green on both Android and iOS (XCFramework) and has a working "hello world" screen wired through the full stack (MVI, Decompose, Koin, design system, data layer).

## Step 0 — gather context

Ask the user (via clarifying questions) the following:

1. **Product name** (e.g. `pulse`). Fills the `<product>` / `<Product>` placeholders everywhere — package root, root project name, namespace prefix.
2. **Organization name** (e.g. `acme`). Used for the package root: `com.acme.pulse.*`. Skip if single-org (`com.pulse.*`).
3. **Backend host** (e.g. `pulse-app.com`) for `BackendClient.defaultRequest.host`. If not yet known, use a placeholder and mark with a TODO.
4. **Application ID** for Google Play (e.g. `com.acme.pulse.android`). Convention: `<package-root>.android`.
5. **First product domain** to model — minimal end-to-end feature (e.g. "Notes", "Tasks", "Bookmarks"). One entity, one feature, one screen.
6. **Locales to support** (e.g. `en`, `uk`). At least one (`en`).
7. **Auth methods** (any combination of: email/password, Google Sign-In, Apple Sign-In, or skip for now).
8. **Firebase** — enabled or skipped? (If enabled, the agent will scaffold the placeholders but leave actual `google-services.json` for the user.)

### Field mapping

| Step 0 question                | 03-project-config.md field        |
|---|---|
| Product name                   | productName, productPackage (lowercased), apiClassName (productName + "Api") |
| Organization name              | productPackage (com.<org>.<product>) |
| Backend host                   | backendHost |
| Application ID                 | applicationId |
| First product domain           | (informational — agents do not read this; used in Step 8) |
| Locales to support             | supportedLocales (YAML list) |
| Auth methods                   | diHandWrittenModules (append GoogleAuthModule / AppleAuthModule per choice) |
| Firebase                       | firebaseEnabled |

Defaults for fields the user is not asked about but MUST be set:

- `iosEnabled` — default `true`; false only if the project intentionally drops iOS.
- `codexEnabled` — default `auto`.
- `prelaunch` — default `true` for a fresh project (room-migration-builder allows destructive fallback). Flip to `false` when the app ships.
- `iosFrameworkName` — default `shared`.
- `typefaceFactory` — name the project picks for its typeface factory function (e.g. `inter`, `roboto`). Default placeholder `<typeface>` until decided.
- `featuresWithRootComponentSuffix` — start as `[]`; the orchestrator updates it when a feature is forced into the suffixed form.

Save the answers AND copy them into 03-project-config.md per the field-mapping table above in Step 1.5. The answers are not just a reference — they become the runtime configuration every sub-agent reads.

## Step 1 — read the requirements

Read in order:

- `requirements/00-overview/*`
- `requirements/01-tech-stack/*`
- `requirements/12-gradle-build/*`
- `requirements/02-module-structure/*`
- `requirements/04-base-classes/*`
- `requirements/05-design-system/*`
- `requirements/06-data-layer/*`
- `requirements/03-architecture-patterns/*`
- `requirements/07-mappers/*`
- `requirements/08-dependency-injection/*`
- `requirements/10-toolkit/*`
- `requirements/11-state-and-formatters/*`
- `requirements/09-conventions/*`
- `requirements/13-anti-patterns/*`
- `requirements/14-cookbook/*`

Don't skim. The architectural rules are interconnected.

## Step 1.5 — populate 03-project-config.md

Open `requirements/00-overview/03-project-config.md`. Replace every value in the YAML frontmatter with the project-specific value from Step 0, per the field-mapping table. For fields the user did not specify, use the defaults noted in Step 0's mapping table.

Verify:
- No value in the frontmatter still reads `<Product>`, `<product>`, `<org>`, `<product-domain>`, or any other placeholder.
- `featuresWithRootComponentSuffix` and `diHandWrittenModules` are empty `[]` or contain only modules the project actually intends to ship.
- `prelaunch: true` for an unreleased project; `firebaseEnabled` matches the Step 0 answer.

Do NOT proceed to Step 2 until 03-project-config.md is fully populated. Every sub-agent invoked later reads this file.

## Step 2 — initialize the build system

Create the following in order:

1. **`gradle/wrapper/gradle-wrapper.properties`** — Gradle 8.10+ (or the version matching `agp = 9.0.1`).
2. **`gradle/libs.versions.toml`** — verbatim from `requirements/01-tech-stack/02-libraries.md`, with versions left unchanged. Adjust catalog keys if you renamed the product.
3. **`gradle.properties`** — verbatim from `requirements/01-tech-stack/03-gradle-properties.md`.
4. **`settings.gradle.kts`** — root settings file. Initially include ONLY:
   ```
   include(":androidApp")
   include(":shared")
   ```
   You will add more modules incrementally.
5. **`build-logic/`** with the convention plugins from `requirements/12-gradle-build/01-convention-plugins.md`. Create:
   - `build-logic/settings.gradle.kts`
   - `build-logic/build.gradle.kts`
   - `build-logic/convention/build.gradle.kts` with the `gradlePlugin { plugins { ... } }` registrations.
   - `build-logic/convention/src/main/kotlin/<each convention plugin>.kt`.
   - `build-logic/convention/src/main/kotlin/com/<org>/<product>/{ApplySafely.kt, ConfigureJvmToolchain.kt, Libs.kt}`.
6. **Run** `./gradlew --version` to verify Gradle/JVM toolchain. Expected: Gradle 8.10+, JVM 19.

## Step 3 — scaffold the foundation modules

In one batch, create the infrastructure modules listed in `requirements/02-module-structure/01-module-graph.md` under "Mandatory infrastructure modules". For each:

1. Create the directory.
2. Create the `build.gradle.kts` (template from `requirements/12-gradle-build/06-representative-builds.md`).
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

Verify after each batch: `./gradlew tasks` should succeed without errors.

## Step 4 — implement the base classes

Implement `:ui-core:foundation` from `requirements/04-base-classes/*`:

- `BaseViewModel`, `BaseComponent`, `BaseScreen`, `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult`, `ResultKey`, `ResultKeys`, `ComponentIdentifier`, `NoneIdentifier`.
- `OperationManager` (interface + impl).
- `ResultManager` + `ResultEmitter`.
- `collectAsStateMultiplatform` (expect/actual).
- `platformAnimation` + `platformStackAnimator` (expect/actual).
- `CoreModule` (`@Module @ComponentScan`).

Use the verbatim code from `requirements/04-base-classes/*` as the starting point.

Verify: `./gradlew :ui-core:foundation:assemble` builds.

## Step 5 — implement the toolkit

Implement each `:toolkit:*` module from `requirements/10-toolkit/*`:

- `:toolkit:context` — `NativeContext` expect/actual, `ContextModule`.
- `:toolkit:logger` — `AppLogger`.
- `:toolkit:serialization` — `Json` provider via `SerializationModule`.
- `:toolkit:date-utils` — `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting`.
- `:toolkit:http-client` — base `HttpClient` + `ApiErrorParser`.
- `:toolkit:theme`, `:toolkit:localization` — `AppTheme.current`, `AppLocale.current` expect/actual.
- `:toolkit:connectivity` — `Connectivity` interface + Android/iOS impls.
- `:toolkit:notification-manager`, `:toolkit:permission-manager`, `:toolkit:link-opener` — minimal stub impls.
- `:toolkit:image-loader` — Coil 3 + Ktor 3.

Verify after each: the module builds and the Koin module is annotated.

## Step 6 — implement the design system

Implement `:design-system:resources:provider`:

- `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon` interfaces/objects.
- `LocalAppColors`, `LocalAppDp`, ... CompositionLocals.
- `StringProvider` interface.
- Initial `strings.xml` in `commonMain/composeResources/values/strings.xml` with one key (e.g. `app_name`).
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

## Step 7 — implement the data layer

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

## Step 8 — implement the first feature

Pick the simplest end-to-end feature (e.g. a list screen for the first product domain).

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
   - `<X>Router` in `:screen-api`.
   - Seven MVI files for the list screen.
   - `<X>RootComponent`.

## Step 9 — implement `:shared`

`:shared` is the composition root. Create:

- `Koin.kt` listing every `<X>Module` so far.
- `RootComponent`, `RootViewModel`, `RootScreen`, `RootDirection`, `RootContract`, `RootState`, `RootLoader`.
- `DialogComponent`, `DialogContentComponent`, `DialogModule`.
- `Deeplink` enum (empty initially or with one entry).
- iOS bridge `RootViewController.kt` (`mainViewController(): UIViewController`).

Verify: `./gradlew :shared:assembleSharedDebugXCFramework`.

## Step 10 — implement `:androidApp`

Create:

- `App.kt` — `Application` subclass with `Koin.init { androidContext(this); androidLogger() }` and (if Firebase) `FirebaseProvider.setup(this)`.
- `MainActivity.kt` — single Activity with `retainedComponent { RootComponent(it, parseDeeplink(intent)) }` and `setContent { root.Render() }`.
- `AndroidManifest.xml` — `<application android:name=".App">`, `<activity android:name=".MainActivity" android:exported="true">` with the main launcher intent filter and deeplink filters if any.
- `build.gradle.kts` — verbatim from `requirements/12-gradle-build/04-android-app-module.md`.
- `proguard-rules.pro` — empty for now (R8 defaults).
- `google-services.json` — placeholder; user will replace.

Verify: `./gradlew :androidApp:assembleDebug`.

## Step 11 — implement `:iosApp`

Create the Xcode project at `iosApp/` with:

- `iOSApp.swift` — `@main` struct calling `FirebaseApp.configure()` and `KoinKt.doInit { _ in }`.
- `ContentView.swift` — wraps `mainViewController()` from `:shared`.
- `Info.plist` — basic.
- `GoogleService-Info.plist` — placeholder.

In Xcode, link `shared.xcframework` (drag-and-drop after running `./gradlew :shared:assembleSharedDebugXCFramework`).

## Step 12 — verify end-to-end

Run:

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both must succeed.

Run the Android app: a screen renders with the toolbar and at least one element from the design system.

Run the iOS app (via Xcode): same screen renders.

If both succeed, the foundation is complete.

## Step 13 — write `CLAUDE.md`

**Precondition.** If you copied `requirements/` from a reference KMP repo, check the new project's root for an existing `CLAUDE.md`. If one is present, delete it — the file you write in this step REPLACES it. Two CLAUDE.md files at the same scope create contradictory rules; Claude Code loads both and applies them in arbitrary order.

```bash
[ -f CLAUDE.md ] && rm CLAUDE.md
```

If the existing CLAUDE.md was authored for your new project (i.e. you didn't import it from a reference repo), STOP and ask the user before deleting — it may contain real project context.

At the project root, create `CLAUDE.md` describing:

- The product purpose (one paragraph).
- Cross-repo rules (if applicable).
- Communication / decision-making conventions.
- Scope discipline ("don't refactor without an explicit request", etc.).
- When to stop and ask (mirror `requirements/13-anti-patterns/02-when-to-stop-and-ask.md`).

This file becomes the high-level guide for future agent/contributor work. The detailed `requirements/` set is the source of truth.

## Step 14 — install sub-agents

After the foundation builds green and the first end-to-end "hello world" feature is in place, install the sub-agent toolkit so ongoing work can be automated:

See `requirements/README.md` section "Sub-agents — install before first use" for the symlink/copy commands. Then verify:

```bash
ls .claude/agents/   # should list orchestrator.md, builders, validators, helpers
bash requirements/sub-agents/lint.sh   # if Prompt 1 from SUBAGENTS_TODO_PROMPTS.md ran
```

Verify `requirements/00-overview/03-project-config.md` was populated in Step 1.5. If you skipped Step 1.5 for any reason, do it now before invoking any sub-agent — they will all fail with BLOCKED otherwise. From here on, drop new tasks at `requirements/tasks/TASK_<N>_<title>.md` and ask the parent Claude session to "run task TASK_N_<title>.md". The orchestrator drives the rest.

## Constraints

- **Build green at every step.** If a step's verification fails, fix the issue before moving on. Don't accumulate broken modules.
- **Use the verbatim code from `requirements/`** as the starting point — substitute the `<org>`, `<product>`, and `<Product>` placeholders consistently with the values gathered in Step 0. Don't invent new code.
- **Follow the seven-file MVI pattern** for every screen and dialog. No shortcuts.
- **One feature in step 8.** Don't try to scaffold multiple features in this bootstrap pass.
- **No tests** — the requirements explicitly opt out by default. Add them as a separate task on user request.
- **Don't add dependencies** beyond what `gradle/libs.versions.toml` declares.
- **Don't modify the requirements/** during bootstrap — they're the contract.
- **Don't skip the toolkit modules** — they're "trivial" but required by the convention plugins and feature code.

## Iteration model

Bootstrap takes 1–3 hours of agent work for the foundation. Run incrementally:

1. After Step 2 (build system) — verify Gradle works.
2. After Step 5 (toolkit) — verify each module builds.
3. After Step 7 (data layer) — verify Database and BackendClient compile.
4. After Step 9 (`:shared`) — verify XCFramework builds.
5. After Step 11 (app shells) — verify both platforms launch.

Don't push to a remote branch until Step 12 passes.

## When you're stuck

If a step fails:

- Re-read the relevant `requirements/` file.
- Check `requirements/13-anti-patterns/01-forbidden-patterns.md` — you may be doing something the architecture forbids.
- Check `requirements/14-cookbook/*` — there's likely a similar recipe.
- If the failure is in convention plugins, re-read `requirements/12-gradle-build/01-convention-plugins.md` and compare verbatim.

Don't improvise. The architecture has reasons for every choice.
```

---

## Outcome

After the agent completes `launch.md`:

- The project has the full mandatory infrastructure (~25 modules).
- One end-to-end feature is wired (`:data-features:<x>`, `:data-mappers:*`, `:ui-screen-features:<x>`).
- Android and iOS both launch and render the feature.
- `CLAUDE.md` exists for future agent context.
- The team can grow from here using `requirements/14-cookbook/*`.

## What this is NOT

- **It is NOT a one-shot perfect bootstrap.** Expect 1–2 follow-up rounds to fix initial bugs (icon assets, Firebase config, signing keys).
- **It is NOT a code generator.** The agent writes the code; the user reviews and adjusts.
- **It is NOT product-aware.** The user picks the first feature; the agent implements the chosen one.
