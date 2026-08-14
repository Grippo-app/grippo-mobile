# TASK 1 — Добавить Home screen

## Source

- Kind: manual
- Type: manual
- Ref: intent-774d0cb4711b71f842de8cb8c18550af71f4875f74655f96e46a77670f4f0522
- Fingerprint: sha256:91b0e627b643525918b0a20a179d18dc1f2053f2320580702793e846d22f74a5

## Goal

Rebuild the app's post-authentication navigation into a persistent bottom-bar shell. A new host feature owns a bottom navigation bar with four tabs — Home, Calendar, Exercises, Profile — where each tab renders its own feature content and keeps its own state/back stack when the user switches tabs. A central protruding dumbbell action button in the bar starts a new training from any tab. The bottom bar itself is a reusable `:design-system:components` widget. Features stay encapsulated: the host composes existing feature roots as tab children and does not absorb their internal logic.

## Inputs

- Existing feature roots to host as tabs: `:ui-screen-features:home` (`HomeRootComponent`) for the Home tab and `:ui-screen-features:profile` (`ProfileComponent`) for the Profile tab.
- Root navigation to restructure: `RootRouter` in `:ui-screen-features:screen-api` (`RootRouter.kt`) and `RootComponent` in `:shared` (`shared/.../root/RootComponent.kt`), which today start post-auth via `navigation.replaceAll(RootRouter.Home)` and push `Profile` / `Trainings` / `Training` / `Debug` on top.
- Training-start entry point: existing `RootDirection.Training(stage)` -> `RootRouter.Training` full-screen flow, with `StageState.Add` for a new session (`RootViewModel.toTraining`, `RootComponent` createChild -> `TrainingComponent`).
- Exercises data source: existing `:data-features:exercise-examples` (`ExerciseExampleFeature`) and the existing `:ui-dialog-features:exercise-example` detail dialog (`DialogConfig.ExerciseExample`) for item taps.
- Design tokens for the new widget: the `AppTokens` facade — `AppColor` / `AppDp` / `AppIcon` groups in `:design-system:core` and `:design-system:components`.
- Assumed — Calendar tab content: the existing `:ui-screen-features:trainings` feature (`TrainingsRootComponent`) (basis: repo — `trainings` renders a Daily/Monthly calendar timeline via `CalendarTrainingBars` / `DailyTrainingsPage` / `MonthlyTrainingsPage`; no other calendar UI exists).
- Assumed — Exercises tab content: a new `:ui-screen-features:exercises` feature with a basic browsable list of exercise examples backed by `ExerciseExampleFeature`, where tapping an item opens the existing `ExerciseExample` detail dialog (basis: repo — no exercises screen exists today, only the `exercise-examples` data feature and the `exercise-example-picker` dialog; the task grants design discretion).
- Assumed — bottom-bar host module: a new `:ui-screen-features:main` feature module owning a Decompose tab navigator that keeps all four tab children instantiated (e.g. `bringToFront` / `ChildPages`) and renders the design-system `BottomBar` widget (basis: task — a "BottomBar feature with the bottom bar and 4 tabs", features stay encapsulated; exact module/component name at builder discretion).
- Assumed — dumbbell action: the center button starts a new training via the existing `RootDirection.Training(StageState.Add)` flow, pushed full-screen over the tab host so the bar is hidden during an active session (basis: repo — existing training-start path).
- Assumed — the Debug entry point is left as it is today (reachable from Home) and is not a tab (basis: task lists only four tabs).
- Assumed — new UI strings are added for every supported locale (basis: project resource convention / `supportedLocales`).

## Acceptance

### Automated

- A reusable bottom navigation bar widget exists in `:design-system:components` with four tab slots plus a central protruding dumbbell action button, styled via `AppColor.*` / `AppDp.*` slots — anchor: new `BottomBar` composable under `design-system/components/**` and the added token slots in `:design-system:core`.
- A new host feature module (e.g. `:ui-screen-features:main`) is registered in `settings.gradle.kts` and `:shared/Koin.kt` and exposes a root component that hosts a Decompose tab navigator with four children — Home, Calendar (-> `:ui-screen-features:trainings`), Exercises, Profile — and renders the `BottomBar` widget — anchors: `settings.gradle.kts`, `shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt`, the host `*RootComponent.kt`.
- The host's tab navigator keeps all four tab children instantiated and switches the active one (`bringToFront` / `ChildPages`, not `replaceAll`) so each tab retains its own back stack — anchor: the host `*RootComponent.kt` navigator configuration.
- A new `:ui-screen-features:exercises` feature module provides a browsable exercise-examples list screen consuming `ExerciseExampleFeature`, where an item tap shows `DialogConfig.ExerciseExample` — anchors: the `ui-screen-features/exercises/**` seven-file MVI set, `ExerciseExampleFeature`.
- `RootRouter` (`:ui-screen-features:screen-api`) and `RootComponent` (`:shared`) are restructured so the bottom-bar host is the post-authentication root (replacing `navigation.replaceAll(RootRouter.Home)`) and the active training session (`RootRouter.Training`) is pushed full-screen over the host — anchors: `screen-api/**/RootRouter.kt`, `shared/**/root/RootComponent.kt`.
- The central dumbbell button starts a new training via `RootDirection.Training(StageState.Add)` — anchors: the host component and `shared/**/root/RootComponent.kt` / `RootDirection`.
- `./gradlew :shared:assembleSharedDebugXCFramework` and `./gradlew :androidApp:assembleDebug` both green.

### Manual

- The bottom bar renders correctly on all four tabs; tapping a tab switches content and preserves the previous tab's scroll / navigation state; the bar hides during an active training session; the design and spacing of the bar and the central dumbbell button are approved.
- Tapping the central dumbbell button visibly starts a new training from any tab.

## Out of scope

- Rich Exercises browsing UX beyond a basic list (search, filtering, favorites, per-exercise analytics) — separate follow-up task.
- Removing or relocating the existing Debug entry point, and adding Debug as a tab.
- Deeplink changes or `Deeplink` enum edits (external contract) — separate task.
- Automated test coverage of the navigation behavior — the project ships without a test source set (CLAUDE.md); runtime behavior is verified manually here.
- no new entries in gradle/libs.versions.toml
- no changes to CLAUDE.md or orchestrator/**
- no changes to build-logic/** convention plugins
- no schema migration (Database.kt version, migrations/*) — separate task
- no TODO/FIXME markers left in code the task claims as done

## Questions

### Q1 — This task touches test-policy escalation surfaces but its Out-of-scope excludes automated tests; how must the mandatory test-certification gate be satisfied?

- (a) **Add navigation tests (expand scope)** — write behavioral/contract tests on the TASK_2 foundation (RootRouter serialization round-trip, RootDirection to RootRouter mapping, MainComponent tab-retention/back-stack) and update the stale Out-of-scope bullet; certification then passes normally.
- (b) **Owner descope of the tests gate** — keep tests out of scope and run descope-task.mjs so the `tests` gate is recorded N/A for this task; navigation behavior stays manual-only.
- (c) **Re-prep TASK_1** — return it to task-prep to reconcile the stale "no test source set" premise and produce a fresh test contract.

**Recommended**: (a) — TASK_2 (install canonical test foundation) was split from TASK_1 to install exactly this foundation, and the change adds real navigation runtime behavior on escalated surfaces (shared public API, shared composition root, navigation-root/deeplink parser, settings module graph) for which the machine test policy requires behavioral-test evidence; a `test-not-applicable` claim is invalid here and the `tests` finalization gate cannot pass with zero test cases.

**Type**: choice
**Options**: a, b, c

#### Answer

a

### Q2 — Acceptance bullet 7 requires `:shared:assembleSharedDebugXCFramework` and `:androidApp:assembleDebug` green, but both fail on PRE-EXISTING project-wide issues outside this task's diff. How should the build gate be satisfied?

- (a) **Provide config, then re-run** — supply a real `androidApp/google-services.json` whose client package_name is `com.grippo.android`, and authorize setting `kotlin.incremental.native=false` in `gradle.properties`; re-run this task so both build gates go green and it ships completed.
- (b) **Split build-gate remediation into a launch-readiness task** — land THIS task's code now (all modules compile, 21 navigation tests PASS with sealed certification, reviewer clean, iOS shared.xcframework built green with the K/N flag) and track the two pre-existing blockers in a separate build-system task; Android assembleDebug is deferred there.
- (c) **Descope the Android assemble anchor** — run descope-task.mjs to drop `:androidApp:assembleDebug` from this task's acceptance (keeping iOS framework + host tests + Android compile), since it cannot pass until Firebase config is populated project-wide.

**Recommended**: (b) — the code is complete and independently verified; neither blocker is caused by or fixable within this task (google-services needs a real Firebase secret; the Kotlin/Native 2.3.21 incremental-compiler ReturnsInsertion assertion fires on unchanged :toolkit:context), so they belong in a project-scaffold task, not blocking a finished tested feature.

**Type**: choice
**Options**: a, b, c

#### Answer

b

### Q3 — This run was provisioned at a base commit that already contains the entire TASK_1 deliverable (feature code + all 21 navigation tests) with a clean working tree, so there is no candidate diff to compile-check, scope, trace, or test-certify; how should the run resolve?

- (a) **Finalize the already-landed commit** — accept `4894712a1` (current `main` tip; the single commit adding all 36 TASK_1 files + the 21-test suite) as the completed deliverable and move TASK_1 todo->done via an owner/manager finalization that records the landed work plus the Q2=b Android build-gate deferral; a `run` turn cannot do this itself (a run never publishes, cannot seal an empty candidate, and cannot certify a zero-byte footprint).
- (b) **Re-provision at the pre-feature base `b3b7e88d9` and re-run** — re-seal this task's base at the commit before the feature so the work reappears as a real candidate diff; a normal run then compiles, seals the 21-test certification, passes an independent review, and finalizes through the deterministic pipeline.
- (c) **Reopen and rebuild from scratch** — discard the committed feature and rebuild under the pipeline; only if the committed code is not trusted (not recommended — it is complete and verified: every `### Automated` bullet is satisfied and all 21 tests are well-formed).

**Recommended**: (b) — it yields genuine sealed certification and an independent reviewer pass over a real diff (exactly what Q2=b's "land ... with sealed certification, reviewer clean" expects) without trusting an out-of-band commit on `main`; choose (a) if you prefer not to rewind `main` and simply want the finished commit recorded as done.

**Type**: choice
**Options**: a, b, c

#### Answer

b

---

## Outcome

**Status**: completed-with-caveats
**Completed at**: 2026-08-14T17:46:21Z
**Reviewer**: internal-reviewer
**Review iterations**: 1

### Build gates

- `:shared:assembleSharedDebugXCFramework` — pass
- `:shared:compileAndroidMain` — pass
- `:androidApp:assembleDebug` — skipped (deferred to TASK_3 per answered Q2=b; pre-existing google-services + K/N blockers, not this diff)
- `tests` — pass

### Runtime verify

- Gate: deferred (manual hint emitted)
- Result: n/a — no Anthropic verify skill in runtime; manual bottom-bar/dumbbell behavior left for on-device owner check

### Acceptance trace

- `A reusable bottom navigation bar widget exists in `:design-system:components` wi` — verified — BottomBar + AppColor/AppDp bottomBar token slots + DarkColor impl
- `A new host feature module (e.g. `:ui-screen-features:main`) is registered in `se` — verified — :main in settings.gradle + Koin.kt MainFeatureModule; MainComponent+MainScreen render BottomBar
- `The host's tab navigator keeps all four tab children instantiated and switches t` — verified — childPages pageStatus selected=RESUMED/others=CREATED; MainComponentTabRetentionTest PASS
- `A new `:ui-screen-features:exercises` feature module provides a browsable exerci` — verified — 7-file MVI VM consumes ExerciseExampleFeature; tap shows DialogConfig.ExerciseExample
- ``RootRouter` (`:ui-screen-features:screen-api`) and `RootComponent` (`:shared`) ` — verified — RootRouter.Main post-auth root; Training pushed full-screen over host via toNav()
- `The central dumbbell button starts a new training via `RootDirection.Training(St` — verified — MainDirection.StartTraining -> toTraining(StageState.Add) -> RootRouter.Training push
- ``./gradlew :shared:assembleSharedDebugXCFramework` and `./gradlew :androidApp:as` — deferred — iOS XCFramework PASS; androidApp assembleDebug deferred to TASK_3 per Q2=b
- `The bottom bar renders correctly on all four tabs; tapping a tab switches conten` — manual — on-device owner verification of rendering, tab switching, back-stack retention, bar hidden in session
- `Tapping the central dumbbell button visibly starts a new training from any tab.` — manual — on-device owner verification of dumbbell start-training

### Caveats

- androidApp:assembleDebug deferred to TASK_3 (Q2=b); blockers are pre-existing.
- iOS XCFramework required -Pkotlin.incremental.native=false for a pre-existing Kotlin/Native 2.3.21 linker bug.
- MainFeatureModule is empty and :exercises keeps the Koin convention (reviewer Minor).

### Follow-ups

- `TASK_3_build_gate_readiness_for_task_1_fix_kotlin_native_ios_incremental_lin_f0c6cff0de` — backlog

### Files touched

- `design-system/components/src/commonMain/kotlin/com/grippo/design.components/bottombar/BottomBar.kt` — created
- `design-system/resources/provider/src/commonMain/composeResources/values-ru/strings.xml` — modified
- `design-system/resources/provider/src/commonMain/composeResources/values-uk/strings.xml` — modified
- `design-system/resources/provider/src/commonMain/composeResources/values/strings.xml` — modified
- `design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppColor.kt` — modified
- `design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppDp.kt` — modified
- `design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/DarkColor.kt` — modified
- `settings.gradle.kts` — modified
- `shared/build.gradle.kts` — modified
- `shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt` — modified
- `shared/src/commonMain/kotlin/com/grippo/shared/root/RootComponent.kt` — modified
- `shared/src/commonMain/kotlin/com/grippo/shared/root/RootDirection.kt` — modified
- `shared/src/commonMain/kotlin/com/grippo/shared/root/RootScreen.kt` — modified
- `shared/src/commonMain/kotlin/com/grippo/shared/root/RootViewModel.kt` — modified
- `shared/src/commonTest/kotlin/com/grippo/shared/root/RootDirectionMappingTest.kt` — created
- `ui-screen-features/exercises/build.gradle.kts` — created
- `ui-screen-features/exercises/src/commonMain/kotlin/com/grippo/exercises/ExercisesComponent.kt` — created
- `ui-screen-features/exercises/src/commonMain/kotlin/com/grippo/exercises/ExercisesContract.kt` — created
- `ui-screen-features/exercises/src/commonMain/kotlin/com/grippo/exercises/ExercisesDirection.kt` — created
- `ui-screen-features/exercises/src/commonMain/kotlin/com/grippo/exercises/ExercisesLoader.kt` — created
- `ui-screen-features/exercises/src/commonMain/kotlin/com/grippo/exercises/ExercisesScreen.kt` — created
- `ui-screen-features/exercises/src/commonMain/kotlin/com/grippo/exercises/ExercisesState.kt` — created
- `ui-screen-features/exercises/src/commonMain/kotlin/com/grippo/exercises/ExercisesViewModel.kt` — created
- `ui-screen-features/main/build.gradle.kts` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainComponent.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainContract.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainDirection.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainFeatureModule.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainLoader.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainRouter.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainScreen.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainState.kt` — created
- `ui-screen-features/main/src/commonMain/kotlin/com/grippo/main/MainViewModel.kt` — created
- `ui-screen-features/main/src/commonTest/kotlin/com/grippo/main/MainComponentTabRetentionTest.kt` — created
- `ui-screen-features/screen-api/build.gradle.kts` — modified
- `ui-screen-features/screen-api/src/commonMain/kotlin/com/grippo/screen/api/RootRouter.kt` — modified
- `ui-screen-features/screen-api/src/commonTest/kotlin/com/grippo/screen/api/RootRouterSerializationTest.kt` — created

### Execution log

- Phases: intake · preflight · planner · builders(6) · validators (1 cycle, clean) · tests (PASS 21/21) · assemble-gate · review (internal, approved)
- Totals: 1 turn · stops 0 · retries 2 (test cert re-run: source-snapshot regen + staging cache rebuild after an errant cleanup; all gates re-verified green)
- Design: none
- Spawned: TASK_3 (backlog) — build-gate readiness follow-up for the deferred androidApp assemble
