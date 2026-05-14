# Invalidate — Iterative Improvement Pass

This prompt instructs an agent to **re-audit and improve** the `requirements/` set against either the reference repo or an in-progress new project. Run it iteratively: each pass should find a small number of high-value improvements without expanding scope.

---

## How to use

Feed the following prompt to your agent (Claude / GPT / etc.) at the **root of the project** that contains the `requirements/` folder. The agent will read the requirements, compare them to the live code, and produce a structured list of corrections.

If the live code is the **reference repo** (`grippo-mobile`), the agent verifies that the requirements faithfully describe the architecture. If the live code is a **new project bootstrapped from these requirements**, the agent flags drift (architecture deviated from the requirements).

Run the prompt multiple times. Each pass focuses on one chapter group; treat them as iterations. The **Audit log** at the bottom of this file is the source of truth — the agent reads it to pick the next chapter (lowest count wins) and updates it after the pass. After every chapter has count = 1, the agent automatically goes round 2 (lowest = 1), then round 3, and so on.

---

## The prompt

```
You are auditing a `requirements/` folder that describes the architecture of a Kotlin Multiplatform (KMP) mobile project. The requirements are organized as numbered chapters under top-level folders (00-overview, 01-tech-stack, ..., 14-cookbook), plus two special files (invalidate.md, launch.md).

Goal: **find and fix inaccuracies, omissions, and drift** between the requirements and the live code in the same repository.

## Step 1 — pick a focus chapter for this pass

Read the **Audit log** table at the bottom of `requirements/invalidate.md`. Pick the chapter with the **lowest audit count**. Ties: pick the one that appears first in the table (top-to-bottom order = lower chapter number first).

Examples:
- All counts at 0 → pick `02-module-structure` (first in table).
- Some at 1, one at 0 → pick the 0.
- All at 2, one at 1 → pick the 1.
- All at 2 → round 3 begins; pick `02-module-structure` again.

If the user explicitly names a chapter in the conversation, honor that instead of the lowest-count rule.

## Step 2 — enumerate scope (no skimming, no sampling)

Build TWO exhaustive lists. Write them out explicitly. Do not proceed until both are complete.

### A. Every requirements file in this chapter

```bash
ls requirements/<chapter>/*.md
```

Every single file. Not just the ones that look interesting. You will read each completely.

### B. Every source file the chapter describes

For each .md file, identify every source artifact it references — Kotlin classes, build scripts, resources, configs. Build a flat list with full paths.

Chapter → source mapping:

- **02-module-structure** → `settings.gradle.kts`, every module's `build.gradle.kts` mentioned in any of the chapter's files (~70 modules in the reference repo)
- **03-architecture-patterns** → `:shared/RootComponent.kt`, every `*RootComponent.kt`, several representative `*ViewModel.kt`/`*Component.kt`/`*Screen.kt`, `:ui-dialog-features:dialog-api/DialogConfig.kt`
- **04-base-classes** → every file under `ui-core/foundation/src/{commonMain,androidMain,iosMain}/kotlin/...` — base classes, models, internal operation/result helpers, platform `expect/actual`s
- **05-design-system** → every file under `design-system/{core,components,resources,preview}/src/commonMain/kotlin/...`
- **06-data-layer** → `BackendClient.kt`, `TokenProvider.kt`, `GrippoApi.kt`, `ClientLogger.kt`, every DTO file under `dto/`, `Database.kt`, every `*Entity.kt`/`*Dao.kt`/`*Pack.kt`, every `Migration*.kt`, `DatabaseBuilder.{android,ios}.kt`, `StringListConverter.kt`, DataStore files
- **07-mappers** → each of the 7 `:data-mappers:*` modules' `build.gradle.kts` + 2–3 representative mapper files per direction
- **08-dependency-injection** → `:shared/Koin.kt`, every `*Module.kt` across the project (grep `@Module` + `@ComponentScan`)
- **09-conventions** → grep the entire repo for anti-pattern markers (`viewModelScope.launch`, `mutableListOf`, `mutableStateListOf`, `Color(0xFF`, `\.dp\b` outside design-system, `stringResource\(R\.`, `painterResource\(R\.`, `runBlocking`, `GlobalScope`, `lateinit var`) — read a few sample files
- **10-toolkit** → every file under `toolkit/*/src/{commonMain,androidMain,iosMain}/kotlin/...`
- **11-state-and-formatters** → every file under `ui-core/state/src/commonMain/kotlin/.../formatters/...` + `UiText.kt`
- **12-gradle-build** → every file under `build-logic/convention/src/main/kotlin/`, `gradle/libs.versions.toml`, `gradle.properties`, `settings.gradle.kts`, `:androidApp/build.gradle.kts`, `:shared/build.gradle.kts`
- **13-anti-patterns** → grep the entire repo for each "forbidden" item; absence is the expected state
- **14-cookbook** → for each recipe, identify the artifacts it touches and verify they exist with the documented shape

Use Bash (`find`, `grep`, `ls`) + Read tool. Do not use Explore subagent — it samples and skims. Spawn an Agent only for breadth searches (>5 grep queries), and require the agent to return verbatim code blocks with file paths and line numbers.

## Step 3 — deep verification per .md file

For EACH .md file in the chapter, perform the following 7-step checklist. Do not skip items. Do not abbreviate. If a step has nothing to check (e.g. a file with no code blocks), state that explicitly — don't silently skip.

### 3.1 — Read the entire .md file once, top to bottom

No skim. No partial read. Note: headers, every code block, every prescriptive list (MUST / forbidden / anti-patterns / rules), every cross-reference to another `requirements/` file, every named API.

### 3.2 — For every verbatim code block, line-by-line compare against the source

Open the source file. Compare:

- Class / interface / object name + visibility modifier
- Generic parameters
- Constructor parameter list (names, types, defaults, `@InjectedParam`, etc.)
- Every method signature: name, parameters, return type, every modifier (`public`, `internal`, `protected`, `suspend`, `inline`, `@Composable`, `@ReadOnlyComposable`)
- Every annotation: `@Single(binds = [...])`, `@Factory`, `@Module(includes = [...])`, `@ComponentScan`, `@Serializable`, `@SerialName("…")`, `@Entity(tableName = "…", indices = […], foreignKeys = […])`, `@PrimaryKey`, `@Transaction`, `@Insert(onConflict = …)`, `@Immutable`, `@Stable`, `@Transient`, etc.
- Every default value in parameters and properties
- Field names, types, visibility, defaults
- Method bodies if quoted in the doc — character-for-character (modulo formatting)
- Comment content if quoted

A single-character mismatch counts. A missing modifier counts. A reordered parameter counts.

### 3.3 — For every prescriptive rule (MUST / forbidden / do NOT / Rules section)

For each rule, grep the codebase for evidence:

- "MUST be `@Immutable`" → grep `@Immutable` in the area. Any data class missing it? Either the doc is wrong or the code drifted.
- "Forbidden: `viewModelScope.launch`" → grep across the repo. Any hit? Either drift or the doc overstates the rule.
- "`@Single(binds = [X::class])`" → grep the actual annotations. Mismatch?
- "`Channel(BUFFERED)`" → check the actual capacity. Reference repo uses `Channel.BUFFERED`?

If the rule is followed everywhere → ✓ no finding. If violated → finding (decide direction: doc fix vs code drift). If partial → AMBIGUOUS finding.

### 3.4 — For every named symbol (class / method / constant / token / module path)

Verify it exists at the documented path with the documented signature.

For tokens (`AppTokens.colors.text.primary`, `AppTokens.dp.screen.horizontalPadding`):

- Open `AppColor.kt` / `AppDp.kt`
- Verify the nested path exists
- Verify the type (`Color`, `Dp`)

For module paths (`:ui-screen-features:profile`):

- Verify `include(":ui-screen-features:profile")` in `settings.gradle.kts`
- Verify the directory exists
- Verify the namespace matches what the doc says

### 3.5 — For every cross-reference to another requirements file

The doc says "see `04-base-classes/05-operation-manager.md`" → open it, verify the content actually covers what the cross-reference promises. Stale references count as findings.

### 3.6 — For every Cookbook step (only for `14-cookbook` chapter)

Mentally walk through each step against the live code:

- File paths referenced — do they exist?
- API calls — do they exist with the documented signature?
- Convention plugin behaviors — do they match what the recipe assumes?
- Final command (`./gradlew ...`) — does it match the current task graph?

Find a step that's stale → finding.

### 3.7 — For every example code block (not labeled "verbatim")

Even illustrative examples must be **valid under the current architecture**:

- Would the imports resolve?
- Do the annotations exist and apply correctly?
- Are called APIs present with that signature?
- Does it violate any anti-pattern in `13-anti-patterns/`?

A non-verbatim example that wouldn't compile is a finding.

## Step 4 — produce a structured diff report

Document **every** finding from Step 3. Classify:

- **MISSING** — the requirements omit something that exists in the code.
- **OUTDATED** — the requirements describe something that no longer matches.
- **INACCURATE** — subtly wrong (wrong name, wrong parameter, mistyped constant).
- **AMBIGUOUS** — multiple interpretations; code commits to one.
- **REDUNDANT** — repeated content within or across files.
- **UNCLEAR** — correct but hard to follow.

Format each finding (be verbose; this report is the audit deliverable):

```
### Finding N: <short title>

**Requirements file:** requirements/<path>.md (line range NN–MM)
**Category:** <one of the six>
**Source evidence:** <module>:<full/path/from/repo/root>:<line> — quote the source line(s) verbatim
**Confidence:** High / Medium / Low

**Doc says** (verbatim quote):
> …

**Code says** (verbatim quote):
> …

**Mismatch:**
- Precise description of the discrepancy.

**Proposed correction:**
- The exact replacement text (so the edit can be applied mechanically).

**Reasoning:**
- Why this is doc drift vs code drift vs intentional aspiration.
```

**No empty audits.** If you found 0 findings, that itself is suspicious for a thorough pass — re-read the chapter and verify you actually checked each of the 7 steps for each file. Some chapters genuinely yield few findings (e.g. `13-anti-patterns` if the codebase is clean), but write down what you checked so the next pass can pick up where this one stopped.

## Step 5 — apply edits

For each **High-confidence** finding, edit the corresponding requirements file. Use targeted `Edit` calls — replace only the affected paragraph (not the whole file).

For **Medium / Low confidence** findings, **do not edit**. Surface them in the report for human review.

After each Edit, re-Read the file briefly to confirm the edit landed cleanly (no broken markdown, no accidentally dropped lines).

## Step 5 — update the Audit log

In `requirements/invalidate.md`, find the **Audit log** table at the bottom and **increment the count** for the chapter you just audited (e.g. `0 → 1`, `1 → 2`). Also fill in the **Last audited** column with today's date (YYYY-MM-DD) and a one-line note (e.g. `Fixed 3 signature inaccuracies in BaseViewModel`).

Use a single Edit on `invalidate.md` to update the row.

## Step 6 — summary

End the pass with a brief summary:

- Chapter audited: ... (now at count N)
- Total findings: N (X high-confidence edits applied; Y flagged for review)
- Next chapter (per Audit log): ... (the chapter with the new lowest count)

## Constraints

- **One chapter per pass.** Don't try to audit everything in one go. A thorough pass on a single chapter takes 30–90 minutes of agent work and should produce 5–30 findings depending on chapter size and current drift.
- **No skimming. No sampling.** Step 2 is "every file in the chapter + every source file it references". Step 3 is "every .md file, every code block, every rule, every symbol". If you find yourself thinking "I'll just check a few", stop and re-read these constraints.
- **No Explore subagent for the audit itself.** Explore samples and reads excerpts; it's wrong for deep verification. Use Read directly. Spawn an Agent only for breadth grep (e.g. "find every `@Single(binds = ...)` in the repo") and require it to return verbatim code blocks with file paths and line numbers — never a paraphrased summary.
- **Don't add new chapters or new files** unless a clear gap is identified that doesn't fit existing chapters.
- **Don't rewrite a file from scratch.** Edit specific paragraphs only.
- **Preserve the document's tone and structure.** Normative voice ("MUST", "do NOT", numbered lists); keep it.
- **No new prescriptive rules** unless the live code already enforces them. Requirements describe what exists, not aspirations.
- **Cite line numbers** when referencing source; cite file paths when referencing requirements docs.
- **Do not delete content** without a strong reason — content that looks redundant may be intentional for cross-referencing.
- **Treat placeholders as not-drift.** The docs use `com.<org>.<product>`, `<Product>Api`, `<your-product-domain>.com` because they're written for a NEW project, not for grippo. Do not flag these as inaccuracies vs the grippo code (where they'd be `com.grippo`, `GrippoApi`, `grippo-app.com`).

## When you're done

Print:

1. The chapter audited and its new audit count.
2. A list of files edited (with one-line summary per edit).
3. A list of findings flagged for human review (Medium / Low confidence).
4. The next chapter to audit (the one that will be picked on the next run per the updated Audit log).
```

---

## Audit log

The agent reads this table on entry to pick the next chapter (lowest count wins; ties broken by table order). After the pass, the agent increments the count and fills in **Last audited**.

| Chapter | Audit count | Last audited (date — note) |
|---|---|---|
| 00-overview | 1 | 2026-05-14 — Fixed :toolkit:* dependency rule (real exceptions are http-client→ui-core:error, date-utils→design-system; firebase wiring is via ui-core:foundation, not toolkit) |
| 01-tech-stack | 1 | 2026-05-14 — Fixed catalog accessor description (dotted path from dashed alias, not camelCase). Flagged: stability config wired but `compose-stability.conf` is missing |
| 02-module-structure | 1 | 2026-05-14 — Fixed app-shell signatures, RootComponent/ViewModel visibility, design-system & ui-core build.gradle, firebase Koin claims, toolkit dep rule, build-logic helper names |
| 03-architecture-patterns | 1 | 2026-05-14 — Fixed State template (default ctor values, not companion Empty); Deeplink wiring (String, not parsed Deeplink); DialogConfig base (`open val`, `protected buildKey`); DialogController has no `dismiss()`; in-sheet uses inner StackNavigation; BaseViewModel.sendError is suspend + FirebaseProvider is static |
| 04-base-classes | 1 | 2026-05-14 — Fixed BaseViewModel error pipeline (FirebaseProvider.recordException, not crashlytics.recordException), onDestroy now shows _navigator.close(), safeLaunch loader is added/removed via invokeOnCompletion (not withLoader), field-injection list trimmed to OperationManager+ErrorProvider; BaseComposeScreen modifier has no fillMaxSize() |
| 05-design-system | 1 | 2026-05-14 — Fixed CompositionLocal location (in :design-system:core internal, not :resources:provider); aligned AppTheme/ProvideResources with single-DarkColor reality; rewrote AppColor representative groups (BackgroundColors, BorderColors, SemanticColors, IconColors, ButtonColors all flat-and-nested-inside-AppColor); replaced AppDp public groups (no public icon/radius/card/toolbar top-level — Screen/Button/Input/BottomSheet/ContentPadding rewritten with real shapes); fixed Button/EmptyState/BannerCard/Chip/SelectableCard signatures; noted Switch/Checkbox/RadioGroup absence; corrected modifier-first rule; flagged missing plurals.xml; fixed drawable conventions (.webp bare-noun, ImageVector icons under AppIcon extensions) |
| 06-data-layer | 1 | 2026-05-14 — Fixed HttpModule (driver() + responseValidator() extension, ContextModule/SerializationModule includes); Json config (useAlternativeNames/prettyPrint instead of explicitNulls/encodeDefaults); TrainingDao naming (get/getById, no @Update, insertOrReplace fan-out); UserExcludedMuscleEntity columns are profileId not userId; DataStore module shape (PreferencesDataStoreBuilder + DataStore<Preferences> injected directly into Repository — no typed *Storage wrappers); range reconciliation example; TrainingBody fields. Flagged: ExerciseResponse missing exerciseExample field in 03 (illustrative, minor) |
| 07-mappers | 1 | 2026-05-14 — Fixed slash-vs-dot dir naming (mapper dirs are slash, not dot); rewrote canonical DTO→Entity mapper to match reality (every field required, profileId ?: userId alias, entity* var prefix); minute-not-millisecond duration unit; SetTraining/SetExercise as toBody sources with plural toBody() not toBodies(); domain-to-state PersistentList + DurationFormatState/DateTimeFormatState + no UiText; state-to-domain returns Set<X>? and uses AppLogger.Mapping per FormatState.value; entity-to-domain documents the @Relation nullable case; dep table adds :toolkit:logger to all 7 rows, drops design-system:resources:provider from domain-to-state; corrected AppLogger.Mapping signature (member, not extension); removed fictional parentId-param pattern |
| 08-dependency-injection | 1 | 2026-05-14 — Removed phantom `firebaseProvider by inject()` from BaseViewModel example (FirebaseProvider is a static object); softened "no hand-DSL" claim to allow `FeatureApiModule` exception; replaced "DatabaseModule is the exception" framing with the broader inline-provider pattern across toolkit/service modules; rewrote Android `App.onCreate` Firebase setup with the 3-arg signature; replaced iOS init example with real `iOSApp` + `AppDelegate` split (`Koin().doInit(appDeclaration:)`; FirebaseApp.configure runs in AppDelegate, not iOSApp init); narrowed `@Module without @ComponentScan` anti-pattern to "for separate annotated classes" |
| 09-conventions | 1 | 2026-05-14 — Fixed State template (default ctor values, not companion Empty) in 01/02; corrected ":design-system:* internal" entry (DarkColor only, no Light); rewrote compose-libs package rows (`com.<org>.<product>.chart.<kind>`, etc., not `compose.libs.*`); fixed AppTokens.colors as member of @Stable object (not extension property); rewrote LaunchedEffect example (key on `systemLocaleTag`, not Unit); narrowed Material3 import rule with allowed exceptions (`Text`, `Icon`, `rememberTooltipState`) |
| 10-toolkit | 1 | 2026-05-14 — Fixed toolkit dep rule (no firebase; real exceptions are http-client→ui-core:error, date-utils→design-system); ContextModule Android resolves Application (not Context) and @Module/@ComponentScan + actual constructor() must be on actuals; rewrote HttpModule (single `HttpClient(context, parser)` provider, includes ContextModule+SerializationModule, PlatformDriver+ResponseValidator extensions, `ApiErrorParser` is `@Single class(json: Json)` with parseDetailedMessage/parseKeys/getDefault*ErrorMessage, no ExpectedErrorBody) and its build deps; Json config (useAlternativeNames/prettyPrint, not explicitNulls/encodeDefaults); logger plugin/dep list (no koin-annotation, no toolkit-context, just libs.datetime), Android log path is user.home, LogFileWriter shape (append(text)+companion create/deleteFile); Connectivity provider is `NativeContext.createConnectivity()` extension with defaulted options + expect `getConnectivityProvider()`; notification-manager namespace is `.notification.manager` + androidResources.enable; permission-manager API uses `check`/`request` returning `PermissionStatus` (Granted/Denied/DeniedPermanently) and `AppPermission` enum (Notifications only); LinkOpener.open returns `LinkOpenResult` and AndroidLinkOpener takes raw `Context`; iOS AppLocale uses `NSLocale.preferredLanguages` (not `currentLocale`) with foreground re-read; localization needs androidx.appcompat on Android; image-loader is `SingletonImageLoader.Factory` + `createdAtStart` initializer (no NativeContext, no SingletonImageLoader.setSafe in App.onCreate), depends on `:toolkit:http-client` |
| 11-state-and-formatters | 1 | 2026-05-14 — Documented 7 missing *FormatState types (DateTime/Name/Percentage/Intensity/Density/Multiplier/Repetitions); fixed PasswordFormatState hint description (6 chars, not 8+digit); fixed VolumeFormatState short()/shortAnnotated() format claims; softened generic factory claim; replaced companion-Empty rule with default-ctor-values (matches actual BaseViewModel(State()) pattern); rewrote stubs section (public top-level in :ui-core:state, not internal-per-feature); cleaned awkward `@Serializable` rationale paragraph |
| 12-gradle-build | 1 | 2026-05-14 — Fixed convention-plugin helpers (real files are `PluginManagerExtensions.kt` and `ProjectExtensions.kt`, not `ApplySafely.kt`/`Libs.kt`); replaced fictional `KotlinTopLevelExtension`-based `configureJvmToolchain` with the real `findByName("kotlin")` + multi-extension type check; corrected `applySafely` parameter name (`pluginId`) and `Project.libs` getter syntax; namespace placeholders in 06 and 01 now match real format (`ui.screen.features.<feature>`, `ui.dialog.features.<feature>`, `design.system.components`) |
| 13-anti-patterns | 1 | 2026-05-15 — Verified every forbidden pattern by grep; relaxed `:toolkit:* → :design-system:*` rule to note the documented `:toolkit:date-utils` exception. Flagged for review: `DialogState.innerConfigs: List<DialogConfig>` (derived field), `SplashViewModel` try/catch as control flow, redundant `@OptIn(ExperimentalForeignApi)` in iosMain (already in global optIn list), `TimeoutCancellationException` catches in TokenProvider |
| 14-cookbook | 1 | 2026-05-15 — Fixed State template (no companion Empty / no @Serializable, default ctor values) in 01/02; rewrote 02 dialog MVI to match real `HeightPickerComponent`/`ConfirmationComponent` shape (public class, `Back`+`BackWithResult`, single `back` callback, no `dismiss()`); fixed `DialogConfig` base (`open val onDismiss`/`dismissBySwipe`, `protected buildKey`) and `AppTokens.dp.contentPadding.large/medium` → `content/subContent` (no such tokens); 04 mapper dirs use slashes (`dto/entity/<area>`, not `dto.entity/<area>`); 04/06 `userActiveDao.get()` returns userId, not profileId — added the two-step `userDao.getById(it).profileId` lookup; 07 noted plurals.xml absence, replaced `ic_*`/`img_*` SVG/XML drawable convention with `.webp` bare-noun + new `Add an icon` section (ImageVector extensions on `AppIcon`); 07 fixed `manrope()` (7 weights, not 5; `AppFont` is a private typealias), removed `AppTokens.dp.icon.medium` (no public `icon` group); 08 made `RootDirection`/`RootContract` `public` (was `internal`), rewrote `eventListener` example to match real `RootRouter.Auth(...)` + `RootRouter.Profile(ProfileRouter.…)` shape, fixed `HomeRootComponent` to `public class` with `initialStack`+`handleBackButton`; 03 `Index(value = ["profileId"])` form. |

After all chapters reach count = 1, the table represents round 1 complete. The next pass picks the lowest (still 1 somewhere or all at 1 → top-to-bottom = round 2 begins). And so on.

---

## Iteration cadence

- **Run after every major code change**: post-refactor, post-version-bump, post-new-feature.
- **Run weekly** in a steady-state project to catch slow drift.
- **Run once at project bootstrap** after `launch.md` finishes, to verify the new project actually matches the requirements.

## What this pass is NOT

- It is **not** a code review. It doesn't propose code changes — only document changes.
- It is **not** a coverage check. It doesn't try to document things the requirements deliberately omit (e.g. specific endpoints, specific entities).
- It is **not** an architectural review. It accepts the existing architecture as correct and verifies the description matches.

## Manual fallback

If you want to run the pass without an agent:

1. Pick one chapter (e.g. `04-base-classes/`).
2. For each file in the chapter, open it side-by-side with the live code it references.
3. Walk through each verbatim code block in the requirements — is it still accurate?
4. Walk through each prescriptive rule — does the code still follow it?
5. Note discrepancies; commit corrections in a single PR.

Cadence: one chapter per session, ~30 minutes per chapter.
