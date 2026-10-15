# Home analytics coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-screen-features:home`, `HomeRouter` in
`:ui-screen-features:screen-api` and `:analytics:home`; replace them when that
code changes. Typed dismissal observations use DismissRequested with
`dismiss_requested` wire names; operation schemas omit visible-screen context and
correlate outcomes by `operation_id`; the app is unreleased and every schema is
version 1.

## Entry path

`RootRouter.Home` builds MainComponent, whose Home tab (`MainRouter.Home`, tab
stack key `MainTabs`, `handleBackButton = false`) constructs
`HomeRootComponent(initial = HomeRouter.Home)`; its stack (`HomeRouter.serializer()`,
key `HomeRootComponent`, `handleBackButton = true`) has exactly one payload-free
route and is always one deep. No other entry path to HomeComponent exists.
HomeViewModel emits only the Trainings, Goal and Back directions, which the root
forwards to Main; the HomeRoot container registers its own BackCallback that
closes without an event.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Presentation identity | Coverage |
|---|---|---|---|
| `HomeRouter.Home`: one surface with three render variants (loader while the persisted range is unresolved, WelcomeHomeContent for a user with no trainings, DashboardHomeContent otherwise; the toolbar title is hidden in the welcome variant) | HomeComponent → HomeViewModel | `HomeScreen.Home` / `home` | One typed screenPresentation covers all variants; HomeAnalytics resolved inside `retainedInstance`; foundation binds before activation and supplies Initial/Restore |
| Home graph | HomeRootComponent → HomeRootViewModel | Default null | Container renders only the child stack; its Contract methods forward to HomeRootDirection; no analytics dependency |
| Overlays opened from Home: ExerciseExample, MuscleLoadingDetails, TrainingStreakDetails, PerformanceTrendDetails, TrainingGoalDetails, PeriodPicker | DialogContentComponent factories → the dialogs' own VMs | Live overlays; no screen hook under the dialog policy | Home reports only open intent and accepted results; the five Home-specific configs are constructed only in HomeViewModel, ExerciseExample also elsewhere |

## Callbacks and operations

| Observation owner / callback | Typed event / operation | Eligibility / wire payload |
|---|---|---|
| HomeViewModel.onOpenTrainings (LastTrainingCard, rendered only with a last training) | TrainingsClicked → `home_trainings_clicked` | Existing unconditional navigation; no parameters |
| onOpenExample (ExerciseSpotlightsCard) | ExerciseClicked → `home_exercise_clicked` | Existing dialog opening; exercise ID excluded |
| onOpenMuscleLoading / onOpenTrainingStreak / onOpenGoalDetails (GoalCard click) | MuscleLoadingClicked / TrainingStreakClicked / GoalDetailsClicked | Only after the nonnull resolved range guard |
| onPerformanceMetricClick(type) | PerformanceMetricClicked → `home_performance_metric_clicked` | Only after the nonnull range guard; metric values/type omitted; the performance cards render only past the lifetime unlock thresholds (two trainings for the duration trend, three for performance trends), so eligibility is also UI-gated |
| onOpenPeriodPicker (HighlightsHeader) | PeriodPickerClicked → `home_period_picker_clicked` | Existing dialog opening; no guard; the picker seeds from the current range kind |
| PeriodPicker `onResult` closure | PeriodConfirmed → `home_period_confirmed`, then SavePeriod | Accepted result delivered after the sheet finishes; the picker emits on every card tap, including the already selected kind; the complete `setRange(result).getOrThrow()` block is decorated |
| onAddGoal (GoalCard update action and the HomeUnlockBanner "Set goal" call to action) and the TrainingGoalDetails `onAddGoal` closure | AddGoalClicked → `home_add_goal_clicked` | Three controls share one event; existing navigation at the callback owner; no reporting context propagated and no control attribution |
| onBack (BackCallback registered by HomeComponent) | DismissRequested → `home_dismiss_requested` | Existing Back callback |
| Range-driven `mapLatest` under the Trainings loader | RefreshTrainings → `home_task_*` {home_operation = refresh_trainings} | Complete `getTrainings(start, end).getOrThrow()` block; waits for the resolved persisted range; a range change cancels the in-flight refresh (interrupted); `Processing.WhileActive` re-runs it after a resume beyond the one-second disconnect grace |
| Startup safeLaunch | RefreshExerciseExamples → `home_task_*` {refresh_exercise_examples} | Complete `getExerciseExamples().getOrThrow()` block; once per fresh VM, outside any loader; the exercise-example first load lives here, not in auth/profile creation |
| onWelcomeConfettiShown | Deliberate omission | Rendering callback from the confetti parade, not a user click |

All ten interaction schemas are version 1 and parameterless; the operation schemas
`home_task_started` [home_operation] and `home_task_completed/failed/interrupted`
[home_operation, duration_ms] use the closed `home_operation` codes
`refresh_trainings`, `refresh_exercise_examples`, `save_period`. All ten HomeEvent
variants, the three HomeOperation values and the one HomeScreen value have VM
consumers and exhaustive encoders. Toolkit decoration owns fresh operation IDs,
monotonic timing, original return values/exceptions and cancellation; `trackEvent`
isolates ordinary reporting exceptions and the failed operation payload drops the
error object. Failed Result values reach the standard error pipeline instead of
being silently discarded by startup refreshes. No entity IDs, selected date
bounds, raw input, measurements or errors are encoded.

`HomeAnalyticsModule` (`@Module @ComponentScan`) binds `HomeAnalyticsImpl` as
`HomeAnalytics` and registers one qualified `home_analytics` provider with the 14
schemas; `shared/.../Koin.kt` lists it once, and the module depends only on
`analytics-api`. Admin consumers of `home_operation` (the `Grippo Home operation`
dimension and diagnostic report selector) are reconciled in the root GA4 review.

## Omissions and privacy

Dialog presentation/close/cancellation for the six overlays, metric type and
selected period attribution, control attribution for the three AddGoalClicked
emitters, the notification permission request and reminder scheduling, the
welcome-status read/write (read once with `first()`; the Celebrated write's
Result is discarded, so a failed write reaches neither the error pipeline nor
analytics) and the metric aggregation use cases are not tracked. Root forwarding
callbacks do not duplicate leaf events; the HomeRoot BackCallback closes without
DismissRequested.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| MainComponent / MainTabNavigation (host) | Active tab including `MainRouter.Home` | Preserve (host-owned) | `MainRouter.serializer()`, key `MainTabs` | Tab Back returns to Home through bringToFront; Home-tab Back falls through to close |
| HomeRootComponent | Child navigation | Preserve | `HomeRouter.serializer()`, key `HomeRootComponent` | Payload-free `HomeRouter.Home` only; depth always one; no route inputs or drafts |
| HomeRootViewModel | HomeRootState | Runtime only | Data object | Stateless container; no supplier |
| HomeViewModel | Accepted range kind | Preserve in the existing durable owner (no StateKeeper copy) | `LocalSettingsFeature.setRange/observeRange` → DataStore `range`; Range ↔ DateRangeKind mappers | Initial `DateRangeFormatState.Empty(kind = Last30Days)` with a null value until the store emits; an absent preference resolves to Last30Days only after that emission; `clearUserSettings` (logout/delete) removes `range`; the picker offers Last7/14/30/60, and `Infinity` is a resolvable persisted kind that yields Invalid with nonnull bounds |
| HomeViewModel | Range date bounds / display | Rebuild | `DateRangeFormatState.of(kind)` through DateRangePresets | Recomputed per emission with the current date/locale; no stale formatted snapshot |
| HomeViewModel | `user` | Rebuild | `observeUser()` | Null emissions are ignored, so the user never clears once set |
| HomeViewModel | `lastTraining`, `muscleLoad`, `streak`, `performance`, `goalProgress`, `spotlights` | Rebuild | `observeTrainings(range)` under flatMapLatest → metric use cases with the caller-provided current time; Room relation query over training/exercise/iteration/exercise_example tables | An empty list resets to defaults; refreshed by RefreshTrainings |
| HomeViewModel | `excludedMusclesCount`, `missingEquipmentCount`, `hasGoal` | Rebuild | Excluded-muscle/equipment and goal observations | Defaults 0/0/false |
| HomeViewModel | `showWelcomeConfetti` | Runtime only | `observeHomeWelcomeStatus().first()` then `setHomeWelcomeStatus(Celebrated)`; DataStore `home_welcome_status`, default Idle | True once per fresh VM only when PendingCelebration; consumed inside WelcomeHomeContent and reset at animation start; no celebration on retained recreation; an unfinished animation is not a draft |
| HomeViewModel / HomeScreen | Loader gating | Runtime only | Trainings loader; `isLoading = range unresolved || ((Trainings loading || user null) && empty state)` | The loader shows until the persisted range resolves for every user, so no period picker is reachable before restoration; returning users see the dashboard while RefreshTrainings is still loading |
| HomeViewModel | Jobs, whileActive subscriptions, notification permission/schedule, dialog sessions, analytics provenance | Runtime only | `Flow.safeLaunch` default WhileActive (one-second disconnect grace); live DialogConfig callbacks; host serializers null | Resume beyond the grace re-emits the current range and repeats RefreshTrainings; a range change cancels the in-flight refresh; no restoration flags, previous-source caches or shadow copies |
| Home dialogs | Unaccepted picker selection; `onResult`/`onAddGoal` closures | Runtime only | Dialog slot/step stacks unserialized; the picker value lives only in the dialog VM | The accepted period goes straight to DataStore through SavePeriod; a result still queued at process death has no durability |

No PersistenceKey, `restoreState`/`saveState`, `@Serializable`, `@Transient` or
`var` member exists in the home module; HomeState is `@Immutable` only and
contains no unfinished screen form, accepted parent draft or local filter without
a persistent owner. The existing DataStore owns the user's accepted selection and
is not duplicated in an archive; no secret enters any state.

## Verification

Source gates for the current revision were reconciled against the route → render
variants → retained VM → typed hook chain, the null container, every callback
guard, parameterless payloads, all operation terminal schemas, toolkit isolation
and the preservation classifications above (read-only audit on 2026-10-15; no
build was part of it). Checks recorded for the implementing revisions
(2026-10-13): `git diff --check` passed; scoped
`:analytics:home:compileAndroidMain :ui-screen-features:home:compileAndroidMain
--offline` passed including common KSP, with the generated HomeAnalyticsModule
containing both the HomeAnalytics binding and the qualified `home_analytics`
schema (static generated output confirms the same today);
`compileKotlinMetadata` was skipped by Gradle and is not counted as evidence. The
follow-up that seeded a fresh VM with an unresolved Empty range (HomeState keeps a
resolved default for previews) passed scoped Android compilation of home and
analytics/home (BUILD SUCCESSFUL, 3s), `git diff --check` and inline source gates;
the home `_task` wire names were later matched against the saved admin
definitions. Existing unresolved global opt-in warnings remain; iOS compilation,
UI preview rendering, device restoration and runtime checks were not run for
those revisions.

Permitted manual checks still unrun: choose a nondefault period, wait for the save,
destroy the lifecycle/InstanceKeeper and construct fresh components with the shell
archive; verify the persisted period and current date bounds, then Back and
re-entry; a fresh installation's Last30Days fallback; pause/resume including the
foundation disconnect grace; pending/failed period saves; range changes cancelling
refreshes; reporter-failure injection; one presentation for loading/welcome/
dashboard; which BackCallback Essenty dispatches on system Back (HomeComponent's
versus HomeRoot's); DataStore first-emission timing; Decompose behavior for
inactive Main tabs and the resulting Restore reason; Room invalidation at runtime;
Firebase delivery of `home_*` and `screen_view` payloads. Dialog unfinished inputs
are expected to disappear. Android saved-state navigation and iOS
completed-background-checkpoint restoration retain their shell limits.
