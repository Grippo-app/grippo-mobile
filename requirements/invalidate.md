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
| 04-base-classes | 3 | 2026-05-15 — Round-3 pass. 03 fixed the "Rules" section — the prior wording (`BaseComposeScreen` is not used inside dialogs ... dialog content is `BottomSheetToolbar` + `Column`) was inverted: every dialog `<X>Screen.kt` (verified across all 25 `:ui-dialog-features:*` modules) wraps in `BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))` and lays out a `Column` of `Spacer(AppTokens.dp.dialog.top)` → centered title `Text` → body → `Spacer(AppTokens.dp.dialog.bottom)` → `Spacer(Modifier.navigationBarsPadding())`; zero dialogs ship a `BottomSheetToolbar` (matches the cookbook round-3 fix). 02 fixed three example-drift issues missed in earlier passes: (a) the `eventListener` example invented `ProfileBodyDirection.OpenSettings`/`OpenWorkoutHistory` but real `ProfileBodyDirection` has only `Back` — replaced with the real `LoginComponent` example (`is LoginDirection.Registration -> toRegistration(direction.email)` + `Home`/`CreateProfile`/`Back`) and noted the stack-owning-root carve-out where `eventListener` calls `navigation.push/replaceAll/pop` directly; (b) the stack-owning `Render()` example showed `ProfileRootScreen(state.value, stack = childStack, /* ... */)` but the real `ProfileComponent.Render()` calls `ProfileScreen(this, state.value, loaders.value, viewModel)` — bare-name convention (the same fix landed in chapter 02 / 09 / 14 round-3); (c) the `retainedInstance` example had a 3-dep `ProfileBodyViewModel(userFeature, weightHistoryFeature, dialogController)` but the real ctor is 6 deps with `dialogController` first, then `weightHistoryFeature`/`userFeature`/`updateWeightUseCase`/`stringProvider`/`notificationManager` — fixed, dropped the redundant `: ProfileBodyViewModel` type annotation, and added a note about `BaseComponent.viewModel` being `protected abstract val` with the subtype inferred from `retainedInstance`. Fixed the `observeResult` example — real call site is `TrainingRecordingComponent`, not the fictional `TrainingRootComponent`, and the real `Remove` branch calls `viewModel.onDeleteExercise(action.id)` (not `removeExercise`). Rewrote the "Root components with their own stack" preamble — bare-name is the **default** (`AuthComponent`, `ProfileComponent`, `TrainingComponent`); `<Feature>RootComponent` is reserved for `:home` and `:trainings` (whose first sub-screens would collide with the bare feature name); `DebugComponent` is the single-screen exception with no inner stack. Flagged for review (Medium): (a) `01-base-viewmodel.md` describes a `Processing.WhileActive` mode for the block-form `safeLaunch` but the implementation's `when (processing) { WhileActive, Infinity -> block() }` collapses both branches to `block()`, so the `processing` parameter is effectively dead on the block form — only `Flow<T>.safeLaunch` honors it; (b) `04-base-models.md` lines 130–148 describe `ComponentIdentifier` / `NoneIdentifier` and an aspirational `TrainingIdentifier.ForExercise` subtype, but `ComponentIdentifier` has zero call-site usages outside the `BaseComponent` ctor declaration itself — interface and `NoneIdentifier` are currently dead code. |
| 05-design-system | 3 | 2026-05-15 — Round-3 pass. 02: fixed two stale non-existent slot references — the "Reading colors at the call site" example used `AppTokens.dp.radius.medium` (the `Radius` scale is `private` inside `AppDp` — only public access is via component-scoped groups like `input.radius`, `bottomSheet.radius`) and `AppTokens.colors.border.divider` (real `BorderColors` has only `default` / `focus`); rewrote with `input.radius` + `border.default` and added a short note about the private `dp.radius.*` path. Also fixed the "Why an interface" example which referenced `border.divider` aliased to `divider.standard` (neither exists; real `DividerColors` only has `default`); replaced with `border.default` reusing `divider.default` (and added an `icon.disabled` derived from `text.disabled` alternative). 08: cleared the cross-chapter flag from 14-cookbook's round-3 audit log — the `BottomSheetToolbar` section claimed it's "Used in dialog `<X>Screen.kt` files instead of `Toolbar`" which is inverted: `BottomSheetToolbar` is rendered ONCE by `shared/.../dialog/DialogScreen.kt` (the dialog host that wraps every `ModalBottomSheet`), and individual `:ui-dialog-features:*` `<X>Screen.kt` files only contribute the body content inside `BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))`. Verified by grep across all 25 dialogs (zero hits on `BottomSheetToolbar` outside the shared host). Fixed `LineIndicator` section — prior description ("Slim progress bar / segmented indicator for charts and onboarding. Configurable orientation and segments.") was wrong; real signature is a horizontal progress bar with `progress: Float`, `colors: AppColor.Charts.IndicatorColors.IndicatorColors`, `barHeight`, `labelSpacing`, optional `startLabel`/`endLabel`/`marker` slots — no orientation flag, no segments array; documented the real shape and noted multi-segment displays are composed by stacking. Flagged for review (Low confidence): (a) `Toggle` composable's parameter order is `(checked, onCheckedChange, modifier = Modifier)` — `modifier` is **last**, diverging from the "modifier first" rule the chapter documents as universal; the rule still holds for every other audited component (`Toolbar`, `BottomSheetToolbar`, `Button`, `BannerCard`, `EmptyState`, `Chip`, `LineIndicator`) so this is a code-side outlier worth fixing in the Toggle signature, not a doc fix. |
| 06-data-layer | 3 | 2026-05-15 — Round-3 pass. 03: replaced "rolling log buffer" in the ClientLogger summary with the real file-sink shape — single append-only `app.log` under `${user.home}/<product>/logs/` (no rotation; cleared via `AppLogger.clearLogFile()`), matching the 02/07/10 audit fixes. 04: rewrote the "Storing access tokens in DataStore" anti-pattern — the FK direction was inverted; `TokenEntity` has no FK, but both `UserEntity` (`UserEntity.id → TokenEntity.id`, onDelete=CASCADE) and `UserActiveEntity` (`UserActiveEntity.userId → TokenEntity.id`, onDelete=CASCADE) reference it, so `tokenDao.delete(userId)` from `TokenProvider.handleRefreshFailure` cascades the user row + active-user marker and is the actual auto-logout trigger. 05: rewrote rule 4 ("All fields non-null") and the matching anti-pattern — false as stated; the reference repo has many entities with nullable scalar fields representing genuinely-optional domain values (`IterationEntity.externalWeight`/`assistWeight`/`extraWeight`/`bodyWeight`/`bodyMultiplier` — only one populated per iteration depending on the exercise variant; `DraftIterationEntity` mirrors this; `GoalEntity.secondaryGoal`/`lastConfirmedAt`; `ExerciseExampleEntity.imageUrl`/`lastUsed`; `ExerciseExampleComponentsEntity` boolean+float feature flags; `DraftTrainingEntity.trainingId` is null for new drafts, non-null for edit; `TokenEntity.access`/`refresh` for partial snapshots during refresh). Documented the real convention: required fields non-null via `AppLogger.Mapping.log` rejection in DTO→Entity, optional fields nullable without `= null` default (mapper makes the choice), `TokenEntity` is the lone `Type? = null` exception because its rows are written from inside `TokenProvider` rather than a DTO mapper. Flagged for review (Medium): (a) `ResponseValidator.kt:64` `handleResponseExceptionWithRequest` `else` branch returns an `AppError.Network.Unexpected` value instead of `throw`-ing it (every other arm in the same `when` throws), so unrecognized exceptions are silently swallowed — code-side issue, not a doc fix; (b) 07-datastore.md line 128 lists `booleanPreferencesKey("onboarding_seen")` / `intPreferencesKey("last_used_range_kind")` as naming examples but the reference repo only uses `stringPreferencesKey` (`range`, `last_goal_suggestion_shown_at`, `home_welcome_status`) — illustrative not load-bearing. |
| 07-mappers | 3 | 2026-05-15 — Round-3 pass. 01 fixed the dep-table preamble: prior claim that `AppLogger.Mapping.log` is used by **every** mapper module was wrong — verified by grep across all 7 directions that the three `:domain-to-*` directions (`:domain-to-entity`, `:domain-to-state`, `:domain-to-dto`) have **zero** AppLogger.Mapping call sites because their source is domain (non-null by construction) so there's nothing to drop. Reworded to make the `:toolkit:logger` build-dep a convention while documenting that actual usage is limited to the four nullable-source directions. This also brings 01 in line with the existing rule in 02 ("**No `AppLogger.Mapping.log`.** Domain models have non-null fields") and 03 ("there is no `AppLogger.Mapping.log` in this direction") for the draft case. 02 fixed two "rolling log" / "rolling file log" references (line 70 in DTO→Entity Rule 1; line 275 in the `AppLogger.Mapping` description) that contradicted doc 03's round-2 update — the file is single append-only, no rotation, cleared via `AppLogger.clearLogFile()` from the debug screen. 02 rewrote the Entity→Domain Rules section line 119 — the previous wording "Entities have non-null scalar columns by design" is now stale after the round-3 fix in `06-data-layer/05` acknowledging legitimately-nullable scalar columns (`IterationEntity.externalWeight/extraWeight/assistWeight/bodyWeight/bodyMultiplier`, `GoalEntity.secondaryGoal/lastConfirmedAt`, `ExerciseExampleEntity.imageUrl/lastUsed`, `TokenEntity.access/refresh`). Reworded to keep the no-null-check rule but anchor it on Room enforcing declared nullability — nullable column → pass-through to nullable domain field (e.g. `IterationEntity.externalWeight: Float?` → `Iteration.externalWeight: Float?`), non-null column → read directly, NPE-on-violation if invariant breaks. Same edit also expanded Rule 2 to explicitly cover enum-string parses (`EquipmentEnum.of(type)`, `MuscleEnum.of(type)`, `GoalPrimaryGoalEnum.of(primaryGoal)`) — previously only `@Relation` cases were named, but the codebase shows enum-string parses are the more common Entity→Domain log path (verified across `:entity-to-domain` muscles/equipment/user mappers). 02 added a row to the function-names table covering `Source.toDraftDomain()` / `Source.toSetDomain()` — the suffixed variants used by `:entity-to-domain` for the draft round-trip (`DraftTrainingPack.toDraftDomain() → DraftTraining`, `DraftIterationEntity.toSetDomain() → SetIteration`); the suffix disambiguates because the same source maps to two distinct domain targets depending on whether the consumer wants the draft or set variant. Same row also notes that `:domain-to-state`'s plural form returns `PersistentList<XState>` (not bare `List<XState>`). Flagged for review (Medium confidence): (a) `ExerciseExampleComponentsMapper.kt:52` in `:entity-to-domain` does `AppLogger.Mapping.log(Unit) { … }` which is a no-op since `Unit != null` — likely a vestigial call meant to be `log(null) { … }` or just a `present(LogCategory.MAPPING, …)` direct call; code-side, not doc-side; (b) `LogCategory` enum has `PROMPT` and `ANSWER` values with **zero** call sites anywhere in the codebase (grep verified) — dead enum members; doc 03 line 128 correctly lists only the categories that actually appear in logs (`MAPPING/NETWORK/NAVIGATION/ERROR/WARNING`), so no doc edit needed, but the enum cleanup is a separate code task. |
| 08-dependency-injection | 3 | 2026-05-15 — Round-3 pass. 01: fixed the stale "70+ modules with 200+ singletons" emphasis line — real counts are 86 modules (`grep -c '^include' settings.gradle.kts`) and ~85 singletons (~60 `@Single` annotations + 25 inline `single { }` blocks inside `FeatureApiModule`); rewrote the bullet to "~85 modules and ~85 singletons" with the grep commands inline so the next pass can re-verify. This aligns with the chapter 02 round-3 correction from "~70 modules" → "~85 modules". 02: rewrote the `@Module` without `@ComponentScan` anti-pattern carve-out — previous wording flattened `GoogleAuthModule`/`AppleAuthModule` (genuine "providers-only" with `@Single internal fun ...` inside the class body) and `FeatureApiModule` (hand-DSL exception that hand-rolls the `.module` property via `@get:JvmName("module") public val module: ModuleObject = module { single { ... } }` and has zero provider methods) into one "providers-only" carve-out. Split them into two distinct shapes — providers-only vs. hand-DSL — so the anti-pattern matches both doc 01 line 3 ("The single production exception is `FeatureApiModule`") and the real FeatureApiModule.kt shape (verified: `@Module` only, no `@ComponentScan`, no `@Single` methods, hand-rolled `.module` property with 25 inline `single { }` UseCase registrations). Flagged for review (Medium): `ContextModule` is an `expect class` with `actual class` per platform (`androidMain`/`iosMain` both `@Module @ComponentScan public actual class ContextModule actual constructor() { @Single internal actual fun providesNativeContext(scope: Scope): NativeContext { ... } }`) — currently undocumented in chapter 08 but described matter-of-factly in doc 03 line 70 ("`androidContext(this@App)` registers the Android `Context` so `ContextModule` can provide `NativeContext`"); adding the expect/actual nuance would be a useful documentation enhancement but isn't a correctness drift. |
| 09-conventions | 3 | 2026-05-16 — Round-3 pass. 01 rewrote the `Imports` order rule — real code uses a single alphabetical group (IntelliJ Kotlin default with `androidx.*` → `com.<org>.<product>.*` → `kotlinx.*` → `kotlin.*`); the previous "stdlib first → third-party → first-party" prescription doesn't match what's actually in the repo (verified across `BaseViewModel.kt`, `GrippoApi.kt`, `Database.kt`, `ProfileBodyViewModel.kt`). 01 fixed the `:ui-screen-features:*`/`:ui-dialog-features:*` visibility row that incorrectly listed "Routers in `:dialog-api`" — `:dialog-api` has zero `*Router` types; dialogs use a single `DialogConfig` sealed class plus `DialogController`/`DialogModule`, and per-dialog navigation lives on `DialogContentComponent.navigation` (the inner `StackNavigation<DialogConfig>` host). Re-spelled the row to keep Routers + Deeplink under `:screen-api` and to document the `:dialog-api` public types separately. 02 replaced the lingering `RecalculateGoalProgressUseCase` example in the Classes table (line 29) with real verb-noun (`DeleteTrainingUseCase`, `UpdateWeightUseCase`, `CreateProfileUseCase`) and noun-only (`TrainingDigestUseCase`, `MuscleLoadingSummaryUseCase`) UseCases — same fictional class was already dropped from the Functions row in round 2 but the Classes row was missed. 03 fixed the "Dotted directory names — legacy" section — the previous list included `:data-mappers/dto-to-entity/.../com/grippo/dto.entity.training/` but `:data-mappers:*` modules use regular slash-separated directories (verified across all 7 directions: `com/grippo/dto/entity/training/`, `com/grippo/entity/domain/training/`, `com/grippo/domain/state/training/`, etc.); replaced with a real dotted-dir example (`:data-features/user/.../com/grippo/data.features.user/`) and added a paragraph noting the convention is applied inconsistently — most `:data-features:*` + roughly half of `:ui-dialog-features:*` use dotting; `:data-mappers:*` / `:toolkit:*` / `:data-services:*` / `:design-system:*` / `:ui-screen-features:*` do not. 04 fixed three example-drift issues: (a) the `Button` signature example claimed "`modifier` after the primary content/state inputs" with `onClick`/`text` ahead of `modifier`, but the real `design-system/components/.../button/Button.kt:120` puts `modifier` FIRST followed by `content`, `style`, `state`, `size`, `onClick`, `textStyle` — same pattern across `Toolbar`, `BannerCard`, `BottomSheetToolbar`; updated the convention to "modifier first" and flagged `Toggle(checked, onCheckedChange, modifier)` as the lone outlier; (b) the `ProfileBodyState` example used fictional `WeightPoint` and `User?` types — real state holds `WeightHistoryState` and `UserState?` (both from `:ui-core:state`); rewrote to match `ProfileBodyState.kt:11-17` (with defaults) and added a one-liner cross-ref to the State-defaults convention; (c) the `Modifier.cardElevation` example used `AppTokens.dp.radius.medium`, but the `AppDp.radius` scale is `private` (only the internal `data object Radius` references it) — public access is component-scoped (`bannerCard.radius`, `wheelPicker.radius`, `tooltip.radius`); replaced with `bannerCardSurface` and added a note. Flagged for review (Medium): the `LazyColumn` example in 04 uses `contentType = { "TrainingRow" }` (string literal), but the two real call sites (`DailyTrainingsPage.kt:76`, `TrainingCompletedScreen.kt:144`) use `contentType = { it::class }` — illustrative, both shapes valid. Flagged (Low): the 04 anti-pattern "Hardcoded `Color(...)`, `12.dp`, `14.sp`" is violated in several feature screens (`CalendarTrainingBars.kt:46` `5.dp`, `TrainingCompletedScreen.kt:84` `0.dp`/`40.dp`, `ProfileCreationScreen.kt:63` `100.dp`, `LoginScreen.kt:79` `80.dp`) — code drift, not doc drift. |
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

---

# Sub-agent auto-calibration

The same pattern as the chapter audit above — but applied to the agent definitions under `requirements/sub-agents/`. As the codebase and the requirements drift, an agent's prompt may reference a renamed class, an outdated rule, a chapter that moved, a builder whose contract changed, or a validator whose grep no longer catches the violation. This pass audits one agent at a time.

The **Sub-agent audit log** at the bottom of this section is the source of truth — same lowest-count-wins rule.

## How to use

Feed the following prompt to your agent at the **root of the project**.

## The prompt

```
You are auditing the `requirements/sub-agents/` set — Claude Code sub-agent definitions that implement and validate changes against the architecture in `requirements/`. The set is organized into three groups: `builders/`, `validators/`, `helpers/`.

Goal: **find and fix drift** between an agent's prompt and (a) the chapters in `requirements/` it cites, (b) the live code it operates on, (c) the other agents it coordinates with.

## Step 1 — pick a focus agent for this pass

Read the **Sub-agent audit log** table at the bottom of this section. Pick the agent with the **lowest audit count**. Ties: pick the one that appears first in the table (top-to-bottom = builders first, then validators, then helpers; within each group, alphabetical).

If the user explicitly names an agent in the conversation, honor that instead.

## Step 2 — enumerate scope

Build THREE exhaustive lists. Write them out explicitly.

### A. The agent definition itself

```bash
ls requirements/sub-agents/<group>/<agent>.md
```

Read the file top to bottom. Note: frontmatter (`name`, `description`, `tools`, `model`), every cited chapter, every grep pattern, every rule, every cross-agent reference.

### B. Every `requirements/*.md` chapter the agent cites

The agent's "Authoritative reading" section lists them. Open each. The audit verifies the agent's prompt is consistent with what the chapter actually says now.

### C. Every source artifact the agent acts on

- Builders: the cookbook recipe + the layer of code they touch.
- Validators: the patterns they grep + the files they read.
- Helpers: the other agents they invoke (for `orchestrator`, `codex-review-loop`, `task-intake`) or the chapter index (for `requirements-lookup`).

For each agent, enumerate the touched paths and verify they still exist with the shape the agent assumes.

## Step 3 — deep verification

For the picked agent, perform every applicable check:

### 3.1 Frontmatter

- `name` matches the file name (without `.md`).
- `description` accurately reflects what the agent now does (post-any-edits).
- `tools` is minimal-but-sufficient. Builders need write tools; validators do not. Helpers vary. A drifted `tools` list is a finding.
- `model` makes sense for the workload (sonnet for most; opus only when the task is judgment-heavy).

### 3.2 Cited chapters still cover the cited topic

For each `requirements/<chapter>/<file>.md` referenced in the agent's "Authoritative reading":

- Open the file. Verify the topic the agent claims it covers is still present.
- Verify the cited section heading still exists.
- If the chapter was reorganized (a topic split, merged, moved), the agent's reference is stale → finding.

### 3.3 Cited rules still match the live code

For builder agents:

- Take the agent's "Steps you MUST perform" list. Walk through one real example end-to-end in the live code. Does each step still apply? Does any step assume code shape that has since changed?

For validator agents:

- Take the agent's grep patterns. Run each against the current repo. Do they catch real violations? Do they produce false positives that the agent doesn't filter?

For helper agents:

- `task-intake`: verify the classification table still maps to existing builders. Verify the prerequisite list is still accurate.
- `orchestrator`: verify the loop order matches the real validator/builder set. Verify the escalation triggers point to current chapters.
- `context-finder`: run each "Common queries" recipe against the current repo. Do they return useful results?
- `requirements-lookup`: verify the keyword → chapter table still resolves to existing files.
- `codex-review-loop`: verify the Codex plugin command + the routing table are current.

### 3.4 Cross-agent references

For each "route to <agent>" mention, verify:

- The target agent still exists.
- The target agent's `description` actually covers the work being routed.
- The handoff data shape (what the orchestrator passes to a builder, what a builder reports back) is consistent on both sides.

### 3.5 Anti-patterns the agent enforces

For each forbidden pattern listed in the agent's "What you MUST NOT do":

- Cross-reference against `requirements/13-anti-patterns/01-forbidden-patterns.md`. If the requirement chapter dropped a rule, the agent's enforcement is stale.
- If the requirement chapter added a rule, the agent may be missing an enforcement — finding.

### 3.6 Output format consistency

Validators all share a structured-findings format. If an agent's "Output format" drifted (different fields, different severity scale), that's a finding — the orchestrator and `codex-review-loop` consume validator outputs by shape.

### 3.7 Builder-cookbook parity

Each builder should cover exactly one cookbook recipe. If `requirements/14-cookbook/*` gained a new recipe with no corresponding builder, that's a gap (new agent might be needed — flag for the user, don't auto-create). If a recipe was removed but a builder still exists, the builder is obsolete (flag).

## Step 4 — produce a structured diff report

For each finding, format:

```
### Finding N: <short title>

**Agent:** requirements/sub-agents/<group>/<agent>.md (line range NN–MM)
**Category:** STALE_CHAPTER_REF | STALE_GREP | STALE_RULE | MISSING_RULE | CROSS_AGENT_DRIFT | FRONTMATTER_DRIFT | OUTPUT_FORMAT_DRIFT | OBSOLETE_AGENT | MISSING_AGENT
**Source evidence:** <what changed in requirements/* or in the live code that invalidates the agent's claim>
**Confidence:** High / Medium / Low

**Agent says:**
> <verbatim>

**Reality:**
> <verbatim from the requirements chapter or the live code>

**Mismatch:** <description>

**Proposed correction:** <exact text to replace, so the edit can be applied mechanically>

**Reasoning:** <why this is agent drift vs intentional>
```

## Step 5 — apply edits

For **High-confidence** findings, edit the agent file. Use targeted `Edit` calls. Preserve the frontmatter format and the agent's normative voice.

For **Medium / Low confidence** findings, do not edit. Surface them.

After each edit, re-Read the file briefly to confirm the edit landed cleanly.

## Step 6 — update the Sub-agent audit log

In `requirements/invalidate.md`, find the **Sub-agent audit log** table at the bottom of this section and **increment the count** for the agent you just audited. Fill in the **Last audited** column with today's date (YYYY-MM-DD) and a one-line note.

## Step 7 — summary

End the pass with:

1. Agent audited (now at count N).
2. Files edited.
3. Findings flagged for human review.
4. Next agent to audit per the updated table.

## Constraints

- **One agent per pass.** A thorough audit takes 15–45 minutes per agent and produces 2–15 findings.
- **No skimming.** Step 3 is "every cited chapter, every grep pattern, every cross-agent reference".
- **Don't rewrite an agent from scratch.** Edit specific paragraphs only.
- **Preserve the agent's normative voice** (MUST / MUST NOT / do NOT).
- **Don't add a new agent** unless Step 3.7 surfaces a clear gap AND the user authorizes it.
- **Don't remove an agent** during this pass. If an agent is obsolete, flag it for the user — removal touches the orchestrator's routing.
- **Don't change `tools` to be more permissive** unless the work genuinely requires it. The principle is least-tool: builders write, validators don't.
- **Don't change `model`** without a clear reason; sonnet is the default.
- **Don't audit the requirements chapters themselves** in this pass — that's the chapter-audit flow above. If a chapter has drifted from the live code, route it through the chapter audit; this pass only verifies that the agent's reference to the chapter is current.
```

## Sub-agent audit log

The auditor reads this table on entry to pick the next agent (lowest count wins; ties broken by table order — builders first, then validators, then helpers; alphabetical within group). After the pass, the auditor increments the count and fills in **Last audited**.

| Agent | Group | Audit count | Last audited (date — note) |
|---|---|---|---|
| `cross-feature-nav-builder` | builders | 0 | — |
| `data-feature-builder` | builders | 0 | — |
| `dialog-builder` | builders | 0 | — |
| `endpoint-builder` | builders | 0 | — |
| `mapper-builder` | builders | 0 | — |
| `resource-builder` | builders | 0 | — |
| `room-migration-builder` | builders | 0 | — |
| `screen-builder` | builders | 0 | — |
| `anti-pattern-scanner` | validators | 0 | — |
| `architecture-validator` | validators | 0 | — |
| `build-validator` | validators | 0 | — |
| `compose-stability-validator` | validators | 0 | — |
| `data-layer-validator` | validators | 0 | — |
| `di-validator` | validators | 0 | — |
| `mvi-contract-validator` | validators | 0 | — |
| `naming-convention-validator` | validators | 0 | — |
| `codex-review-loop` | helpers | 0 | — |
| `context-finder` | helpers | 0 | — |
| `orchestrator` | helpers | 0 | — |
| `requirements-lookup` | helpers | 0 | — |
| `task-intake` | helpers | 0 | — |

After all agents reach count = 1, the table represents round 1 complete. The next pass picks the lowest (top-to-bottom in case of a tie → round 2 begins). Same iteration model as the chapter audit above.

## Coordination between chapter audit and sub-agent audit

Run the **chapter audit** first when:

- The live code has changed significantly (new feature, refactor, version bump).
- A sub-agent flags "this chapter doesn't say what I claim it says" — that's a chapter audit signal.

Run the **sub-agent audit** after the chapter audit settles, or when:

- A task execution surfaced sub-agent confusion (a builder did the wrong thing, a validator missed a violation, the orchestrator routed incorrectly).
- A new cookbook recipe was added (Step 3.7 of the sub-agent audit covers parity).
- Codex feedback consistently uncovers issues the internal validators should have caught — a validator's grep is stale.

The two audits never run concurrently — chapter audit owns `requirements/<chapter>/`, sub-agent audit owns `requirements/sub-agents/`, but a sub-agent audit reads chapter files to verify references.
