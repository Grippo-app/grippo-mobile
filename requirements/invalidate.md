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
| 00-overview | 3 | 2026-05-15 — Round-3 pass. Fixed `AppLogger` file-path claim in glossary: replaced product-literal `~/grippo/logs/app.log` (Android) and `NSTemporaryDirectory()/grippo/logs/` (iOS) with the real `${user.home}/<product>/logs/app.log` (java.io.tmpdir/`/tmp` fallback) and `NSTemporaryDirectory()/<product>/logs/app.log` (filename was missing); added "single append-only file, no rotation; cleared via `AppLogger.clearLogFile()`" to align with the 07/10 audit fixes. Flagged for review: (a) glossary line 59 lists `RootComponent` as "should be renamed" but every other audited chapter (03, 14) uses `RootComponent` literally as an infrastructure name; (b) `StringProvider` glossary entry omits `plural(id, quantity, vararg args)`; (c) 02-architecture-overview.md line 93 and glossary line 16 use `GrippoApi` literal where surrounding placeholder convention would say `<Product>Api` — but other chapters (03, 06) keep `GrippoApi` literal so this is broader doc-wide inconsistency, not 00-scope drift. |
| 01-tech-stack | 3 | 2026-05-15 — Round-3 pass. Doc 01's kotlinx-datetime row still listed `Instant` as a kotlinx-datetime type even though zero files import `kotlinx.datetime.Instant` (the codebase uniformly uses `kotlin.time.Instant` — verified at `toolkit/date-utils/.../DateTimeUtils.kt:20`, `toolkit/logger/.../LogEntry.kt:4`, plus `kotlin.time.Clock` at the same call sites and `compose-libs/konfetti/...`). The chapter 10-toolkit doc was corrected for the same kotlin.time vs kotlinx-datetime drift in its round-2 pass; this row was missed. Fixed: kotlinx-datetime row now lists `LocalDateTime`/`LocalDate`/`DatePeriod`/`DateTimePeriod`/`TimeZone`/`Month`/`DayOfWeek` (the actual types imported from kotlinx-datetime in the codebase) and pulls `Instant`/`Clock`/`Duration` into the `kotlin.time` parenthetical. Re-flagged: stability config wired in `ComposeMultiplatformConventionPlugin.kt:29` but `compose-stability.conf` still missing at repo root — silently ignored by the compose compiler plugin; code-side fix, not a doc fix. |
| 02-module-structure | 4 | 2026-05-15 — Round-3 pass. 01 fixed the "~70 modules" claim → "~85 modules" with a `grep -c '^include' settings.gradle.kts` how-to (real count: 86, dominated by 25 `:ui-dialog-features:*` modules) and called out the module-level `compose.resources { ... }` / `androidLibrary { androidResources.enable = true }` blocks that intentionally live outside the convention plugins (`:design-system:resources:provider`, `:toolkit:notification-manager`). 02 fixed hard rule 4 — the previous wording (`MUST NOT depend on UI`) banned the `:data-mappers:domain-to-state` / `:state-to-domain` → `:ui-core:state` edge that both real modules use. Reworded to ban UI feature/dialog modules and document the pure-type `:ui-core:state` back-edge as a deliberate exemption, paralleling hard rule 5's `:toolkit:http-client` → `:ui-core:error:error-provider` carve-out. 04 fixed the `Render()` example to mirror real `RootComponent.Render` — locals `systemIsDark` / `systemLocaleTag` are read once via `AppTheme.current` / `AppLocale.current` and reused both inside the `LaunchedEffect` key/body and inside `AppTheme(...)`; added the rationale (don't re-enter the `@Composable` snapshot reader from a non-`@Composable` `LaunchedEffect` body) and updated the prose reference from `LaunchedEffect(AppLocale.current)` to `LaunchedEffect(systemLocaleTag)`. 07 rewrote the feature-root-naming section to match reality: bare-name (`<Feature>Component` / `<Feature>Screen`) is the **default**; `<Feature>Root*` is reserved for features whose first sub-screen reuses the feature name (only `:home` and `:trainings` in reference repo). Added a second File-Layout block illustrating the `*Root*` case alongside the bare-name `:profile` case, dropped the bogus `ProfileRootComponent.kt` from the canonical example. Fixed the visibility-rule contradiction — feature-root component is `public` (consumed from `:shared`); sub-screens stay `internal`. 08 dropped the fictional `RecalculateGoalProgressUseCase` example → real composing use cases (`TrainingDigestUseCase`, `MuscleLoadingSummaryUseCase`, `LoginUseCase.executeEmail/executeGoogle/executeApple`). 11 fixed the `:toolkit:logger` file-sink description — single append-only `app.log` under `${user.home}/<product>/logs/` (with `java.io.tmpdir`/`/tmp` fallback), no rotation; cleared via `AppLogger.clearLogFile()` (was: "rolling log" with grippo-specific path). Added a `:toolkit:notification-manager` note about the `androidLibrary { androidResources.enable = true }` block that the build script actually uses (icon drawable). 3 (now at count 4) |
| 03-architecture-patterns | 3 | 2026-05-15 — Round-3 pass. 02: corrected the "Three layers of navigation" top-level feature list (`Auth`, `Home`, `Trainings`, `Profile`, `Training`, `Debug` — was missing `Trainings`/`Debug` and used the stale `Authorization` literal); rewrote layer-2's name claim to allow both `<X>Component` and `<X>RootComponent` (was: only `<X>RootComponent`). Replaced the inverted naming-convention paragraph at the bottom of `createChild` — bare `<X>Component` is the **default**; `<Feature>RootComponent` is reserved for `:home`/`:trainings` to avoid colliding with same-named first sub-screens. Owning a private `StackNavigation<<X>Router>` is orthogonal: `AuthComponent`, `ProfileComponent`, `TrainingComponent` all have inner stacks despite the bare name; only `DebugComponent` is the rare single-screen feature with no inner stack. (Prior wording falsely grouped `AuthComponent`/`ProfileComponent`/`TrainingComponent` with `DebugComponent` as "flatter shape".) Fixed the `RootComponent` typical-uses bullet — `RootRouter.Login` doesn't exist; logout uses `RootRouter.Auth(AuthRouter.AuthProcess)` (matches the eventListener fix from round 2). 03: rewrote the "Rules summary" line that contradicted the chapter — "**In-sheet multi-step lives in `State`, not Decompose nav**" was inverted; cross-dialog flows actually push onto `DialogContentComponent.navigation` (the inner `StackNavigation<DialogConfig>`), and only single-dialog multi-mode (radio-button-like) lives in `State`. 06: replaced fictional `RecalculateGoalProgressUseCase` example with real composing UseCases (`TrainingDigestUseCase` + `MuscleLoadingSummaryUseCase` for metrics aggregation; `LoginUseCase.executeEmail/executeGoogle/executeApple` for the auth-then-bootstrap composition). 07: replaced the same `RecalculateGoalProgressUseCase` block in "Inside a UseCase" with a real `LoginUseCase.executeEmail` example and documented the two coexisting patterns: bare `getOrThrow()` chain (caller wraps in `safeLaunch`) vs. outer `runCatching { … getOrThrow() }` when the use case itself must return `Result<T>`. Flagged for review (Medium confidence — illustrative drift, not a load-bearing inaccuracy): 01-mvi-contract.md's `ProfileBodyDirection`/`ProfileBodyLoader`/`ProfileBodyContract` Empty / `ProfileBodyViewModel` examples invent subtypes (`OpenSettings`, `OpenWorkoutHistory`, `LoadingHistory`, `SavingWeight`) and a 3-arg VM constructor that don't match the real 6-arg `ProfileBodyViewModel(dialogController, weightHistoryFeature, userFeature, updateWeightUseCase, notificationManager, stringProvider)` with a single `Back` direction and `ApplyBodyChanges` loader — a developer cross-referencing the real file would be confused. |
| 04-base-classes | 2 | 2026-05-15 — Fixed BaseViewModel `update` description (no `(state as MutableStateFlow)` cast — real impl uses private `_state.update(...)`); minimal VM template now uses `BaseViewModel(FooState())` default-ctor pattern (was `FooState.Empty`); 04-base-models `Action : BaseResult` example replaced — `Action` is a plain sealed interface, every payload is wrapped by `sendResult` in `Result<T>` (matches the audited 03-architecture-patterns story), fixed `Sync(val exercise: Exercise)` → `Sync(val exercise: ExerciseState)` and dropped the bogus two-pattern framing; `Don't extend BaseResult outside screen-api` rule rewritten — nobody extends `BaseResult`, only `Result<T>` does; 02-base-component root-stack example rewritten to match real `ProfileComponent` (`public class`, `initialStack = { listOf(initial) } + handleBackButton = true`, `BackCallback` for back, `Render()` delegates stack rendering to `ProfileScreen`). |
| 05-design-system | 2 | 2026-05-15 — 01: replaced `Res.drawable.ic_settings` + `AppTokens.dp.icon.medium` example with real `Res.drawable.barbell` + `AppTokens.dp.equipmentCard.icon` (icon path is private); separately illustrated `AppTokens.icons.<Name>` for ImageVector. 02: corrected `AppPalette.NeutralDark.N100..N800` → `N050..N800` (with intermediate halves) and noted the `Gradient` bucket. 04: heading scale `30/26/22/20/18/16` → `30/25/22/18/16/15` (real values); rewrote `AppFont` description — it's a file-private `typealias AppFont = Res.font` inside `AppTypography.kt`, not a public type. 05: replaced the `Res.drawable.ic_settings` Icon example with a realistic `Image(Res.drawable.barbell)` block, removed the bogus `AppTokens.dp.icon.medium`. 06: rewrote `RootScreen` example — `AppTheme` actually wraps `RootScreen` + `dialogComponent.Render()` from inside `RootComponent.Render()`, not from RootScreen itself; updated to use `dialogComponent` (not `dialog`) and added the `:toolkit:theme`/`:toolkit:localization` shape. 08: replaced Toolbar trailing `Res.drawable.ic_settings` with the real `Button(content = ButtonContent.Icon(...))` shape; rewrote the `Chip(...)` signature — real signature has 9 params (modifier, label, value, stype, trailing, size, textColor, iconColor, brush) with no defaults beyond `modifier`, and the parameter is `stype` (not `style`) tracking the typo'd type name `ChipStype`. |
| 06-data-layer | 2 | 2026-05-15 — Added `exerciseExample: ExerciseExampleResponse? = null` to `ExerciseResponse` in 03 (carried-over flag); corrected DAO rules 3 & 4 in 05 — `@Update` IS used (in `TokenDao`, `GoalDao`, `UserDao`, `EquipmentDao`, `MuscleDao`); documented the two upsert patterns (REPLACE for child/replaceable aggregates, read-then-`@Insert`-or-`@Update` for parents whose children CASCADE, paired with a `@Transaction insertOrUpdate(...)` default-method); softened `Comments delimit each table` rule in 06 — applies to multi-table recreate-table migrations like `Migration4To5`, not to single-table or add-only ones; reordered entities list in 04-database.md to match real `Database.kt` order (User block + WeightHistory live after Drafts, not after UserActive) |
| 07-mappers | 2 | 2026-05-15 — Re-flagged the parentId-param pattern as the canonical `:data-mappers:domain-to-entity` shape (drafts have no wire payload, so `Uuid.random()` generates the id client-side and parents hand it down): updated 01's `:domain-to-entity` table row to note `Set<X>` is also a source (no `DraftIteration` domain class; iteration is leaf) and that parents return `Draft<X>Pack` while leaf rows return bare `Draft<X>Entity`; added the missing Domain → Entity canonical pattern in 02 with `DraftTraining.toEntity(profileId)` / `DraftExercise.toEntity(trainingId)` / `SetIteration.toEntity(exerciseId)` and the "no `AppLogger.Mapping.log` in this direction" rule; extended the function-names table in 02 with a `toEntity(parentId)` row covering the draft case; added the same exception block to 03's "Composing nested mappers" section so the "no parentId param" rule reads correctly as DTO-only. Fixed log-file path claim in 03 — Android resolves to `${user.home}/<product>/logs/app.log` (java.io.tmpdir/`/tmp` fallback), not "app cache dir"; replaced "rolling file" with "single append-only file" (no rotation; cleared via `AppLogger.clearLogFile()`). |
| 08-dependency-injection | 2 | 2026-05-15 — Fixed doc 01's inline-provider list: `GoogleAuthModule`/`AppleAuthModule` are NOT `@ComponentScan` modules (verified — only `@Module(includes=...)`); split them into their own "providers-only" paragraph cross-referencing 02's anti-pattern carve-out. Corrected the canonical `TrainingsRepository*`/`TrainingsFeature*` examples → singular `TrainingRepository`/`TrainingFeature`/`TrainingFeatureImpl` (module name stays plural `TrainingsFeatureModule`); added an explicit note about the plural-module/singular-impl convention; same fix applied to 02's `@Single(binds=...)` block, the hand-DSL anti-pattern line, and 03's test-override example (`FakeTrainingFeature`). Updated the ProfileBodyComponent `retainedInstance { ... }` example to match the real 6-arg constructor (`dialogController` first, `notificationManager` last) and dropped the redundant `: ProfileBodyViewModel` type annotation since `BaseComponent.viewModel` is `abstract val` and the type is inferred from `retainedInstance`. |
| 09-conventions | 2 | 2026-05-15 — Fixed Repository constructor pattern column in 02 (`<X>Api`/`<X>Dao`/`<X>DataStore`, not `<X>Service`); replaced fictional `RecalculateGoalProgressUseCase` example with real `DeleteTrainingUseCase.execute(id)` / `UpdateWeightUseCase.execute(value)` / `LoginUseCase.executeEmail/executeGoogle/executeApple` (and noted domain-named variants are allowed when a single verb fits poorly, e.g. `TrainingTimelineUseCase.trainingTimeline(...)`). In 03, corrected `internal/` sub-package example (`toolkit.http.client/`, not `toolkit.http/`; added `PlatformDriver.kt`; renamed `DefaultResponseValidator.kt` → `ResponseValidator.kt` to match the real file). Rewrote the per-feature MVI package example — feature-root files are `ProfileComponent`/`ProfileScreen` (bare feature name), not `ProfileRootComponent`/`ProfileRootScreen`; documented the `<Feature>Root*` exception used by `:home`/`:trainings` to avoid collision with same-named sub-screens. In 04, fixed the `LaunchedEffect` example to key on `systemLocaleTag` (matches the actual `RootComponent.Render` call site, not the doc-internal contradiction of `state.range`/`state.localeTag`). In 01, expanded the `:ui-screen-features:*`/`:ui-dialog-features:*` visibility row to call out that the feature-root MVI files are `public` (consumed from `:shared`) while sub-screens stay `internal`. Softened the blanket `runCatching` rule to apply only when the underlying op doesn't already return `Result<T>` (DataStore writes wrap; API-backed repository methods don't double-wrap). |
| 10-toolkit | 2 | 2026-05-15 — Fixed kotlinx-datetime attribution in 06-date-utils.md opening (Instant/Duration/Clock are kotlin.time stdlib, not kotlinx-datetime; expanded the kotlinx-datetime list to the real imports). Fixed permission-manager namespace placeholder (`.toolkit.permission.manager`, not `.toolkit.permission`) and replaced the comment-only androidMain dep with the real `libs.androidx.activity.compose`. Dropped the empty `androidMain`/`iosMain.dependencies` blocks from connectivity's Build section (real module only has `commonMain` deps; engines reach platform APIs directly in actuals). Added `@OptIn(InternalComposeUiApi::class)` to the iOS `AppTheme` example in 11-theme-and-localization.md (LocalSystemTheme/SystemTheme are InternalComposeUiApi-gated, not in the global opt-in list) + note. Reworded the AppCompatDelegate paragraph — `getApplicationLocales()` is API-level-agnostic; AppCompat backports the per-app locale storage on pre-Android 13 (was previously inverted as "Android 13+ only"). Rewrote the 12-image-loader.md Usage example — `AppTokens.dp.icon` is `private` inside `AppDp` and `Res.drawable.img_avatar_placeholder` doesn't exist; replaced with the real `ExerciseExampleImage` pattern (component-scoped dp group + `AppTokens.icons.QuestionCircle` ImageVector fallback + bare-noun `.webp` convention). |
| 11-state-and-formatters | 2 | 2026-05-15 — Fixed WeightFormatState range examples (real `30f..150f`, not `20f..500f`; out-of-range example `200kg`, not `1000kg`; noted 1-decimal normalization); rewrote `Empty` semantics in 02 — numeric formatters collapse `0` to `Empty` (no `Valid(0)`), so `Empty` covers untouched + explicitly zeroed. Fixed HeightFormatState comment in 02 (real reference uses cm `100..250`; mention of "inches" was misleading — unit choice is product-level). Tightened DurationFormatState description — `display` is produced by `DateTimeUtils.format(duration)` (locale-aware abbreviated, e.g. `1h 23m`), parser accepts ISO-8601 via `Duration.parse`, normalized to whole minutes. Rewrote `Process-death safety` in 03: replaced fictional `ImmutableMap<String, String>` example with the real `@Serializable(with = ImmutableListSerializer::class)` pattern (the project ships no ImmutableMap serializer); pulled `UiText` out of the "fields that are @Serializable" list — `UiText` is `@Stable` only (wraps a non-serializable `StringResource`) and must not appear inside router/dialog payloads. Added a `Serialization` section + matching anti-pattern to 01-ui-text.md mirroring that rule. |
| 12-gradle-build | 2 | 2026-05-15 — Fixed stale `Libs.kt` reference in 01's anti-patterns (helper is `ProjectExtensions.kt`, exposed via `Project.libs`; prior pass missed this line). Rewrote 04's `AndroidManifest.xml` description to match the real one — single Activity with default `MAIN`/`LAUNCHER` filter, no deeplink filters, no `<uses-permission>`, no `<meta-data>` (the `GOOGLE_SERVER_CLIENT_ID` manifestPlaceholder is set in `defaultConfig` but unreferenced in any manifest file). Fixed 02's catalog-accessor explanation — Gradle converts hyphens in the alias to `.`, it does not "camelCase the dotted form" (the prior pass fixed an adjacent kebab-case claim but left this one). Flagged for review: chapter 12 scope per `invalidate.md` includes `gradle.properties` but no .md file documents the JVM heap / `kotlin.native.binary.gc=cms` / `org.gradle.workers.max=1` / `org.gradle.configuration-cache=true` settings that live there. |
| 13-anti-patterns | 2 | 2026-05-15 — Architecture-shape rule was overly absolute: documented the real exceptions that let UI features import `:data-services:firebase` (analytics events from VMs), `:data-services:google-auth`, and `:data-services:apple-auth` (consumed by `:ui-screen-features:authorization`). The `TimeoutCancellationException` catches in `TokenProvider` are NOT a violation — both branches propagate (`throw e` on the wait-side, `handleRefreshFailure(e): Nothing` on the refresh-side), so the `CancellationException` rule still holds. Re-flagged as code drift: `DialogState.innerConfigs: List<DialogConfig>` (Kotlin default `List` in `@Immutable` state instead of `ImmutableList`), `SplashViewModel` `try { … } catch (_: Throwable)` translating a domain failure into a navigation decision (arguably a "domain boundary" carve-out per the Coroutines rule, but the rule's exception clause only names `Result.onSuccess`/`runCatching`), redundant `@OptIn(ExperimentalForeignApi::class)` in 3 iosMain files (already in global optIn). |
| 14-cookbook | 2 | 2026-05-15 — 02: rewrote `RatingPickerScreen` example to match the real dialog convention — `BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))` + `Spacer(AppTokens.dp.dialog.top)` + centered title `Text` + body + `Spacer(AppTokens.dp.dialog.bottom)` + `Spacer(Modifier.navigationBarsPadding())` (horizontal padding via `AppTokens.dp.dialog.horizontalPadding`); zero existing dialogs ship a `BottomSheetToolbar`. Replaced the "Using `Toolbar` instead of `BottomSheetToolbar`" Common-mistake item with the real anti-pattern (don't wrap in `Column`, don't reach for `BottomSheetToolbar`, don't use `contentPadding.content` for horizontal). 03: dropped `implementation(projects.toolkit.logger)` from the new data-feature `build.gradle.kts` example — `:toolkit:logger` is a mapper dep (`AppLogger.Mapping`), not a feature-module dep; verified by grep across all `:data-features:*/build.gradle.kts` (zero hits). 08: rewrote step 1 `RootRouter` example to match real shape (`Auth(value: AuthRouter)` data class, plus `Trainings`/`Debug` data objects) instead of the stale `Authorization` data object; added a one-liner about `data class` vs `data object` discrimination. 08 step 4: added the `Login` re-push guard (`if (childStack.value.active.instance !is Child.Authorization)`) that the real `RootComponent.eventListener` uses to avoid stacking auth flows when the token observer fires inside Auth. 01: fixed two stale `ProfileRootComponent` references (`ProfileComponent.createChild` and the `key = "ProfileComponent"` Common-mistake item) — Profile uses the bare-name pattern, not `*RootComponent`. Flagged for review (out of cookbook scope): `05-design-system/08-shared-components.md:69` still claims `BottomSheetToolbar` is "Used in dialog `<X>Screen.kt` files instead of `Toolbar`" — no dialog actually uses it. |

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
