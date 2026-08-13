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
