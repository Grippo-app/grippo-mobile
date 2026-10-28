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
correlate outcomes by `operation_id`; the typed catalog is not yet in a store build and every schema is version 1.

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
| `HomeRouter.Home`: one surface with three render variants (HomeSkeleton for initial range/user/metrics readiness or an active trainings refresh without period content, WelcomeHomeContent for a ready user with no trainings, DashboardHomeContent otherwise; welcome owns its status-bar inset without an empty toolbar) | HomeComponent → HomeViewModel | `HomeScreen.Home` / `home` | One typed screenPresentation covers all variants; HomeAnalytics resolved inside `retainedInstance`; foundation binds before activation and supplies Initial/Restore |
| Home graph | HomeRootComponent → HomeRootViewModel | Default null | Container renders only the child stack; its Contract methods forward to HomeRootDirection; no analytics dependency |
| Overlays opened from Home: ExerciseExample, MuscleLoadingDetails, TrainingStreakDetails, PerformanceTrendDetails, TrainingGoalDetails, PeriodPicker | DialogContentComponent factories → the dialogs' own VMs | Live overlays; no screen hook under the dialog policy | Home reports only open intent and accepted results; the five Home-specific configs are constructed only in HomeViewModel, ExerciseExample also elsewhere |

## Callbacks and operations

Dashboard cards use concise `Overview` presentations. The existing card actions
open MuscleLoadingDetails, TrainingStreakDetails, PerformanceTrendDetails and
TrainingGoalDetails. Detail dialogs provide the corresponding period evidence
and diagnostics; the muscle dialog retains its full distribution and offers the
PerGroup/PerMuscle selector when a known group distribution exists. Overview
card headings use the same primary style and decorative top-right chevron, with
their existing parent click as the sole detail action; Calendar and Update retain
their explicit callbacks.
Performance metrics retain their order, individual click owners and null-item
omission; each pair shares a natural-height row to align card boundaries.
Goal Overview uses the current goal and selected analysis window. An unavailable
score stays neutral; an empty period preserves an existing goal, and removing the
goal clears its card. Details show the same grouped findings that produce the
score (100/50/0 for assessable criteria); unknown criteria are omitted. Evidence
includes analyzed days, sessions/sets, excluded observations and missing metadata.
Goal details use an open compact summary, up to two attention findings and one
positive finding; the remaining checks and calculation details are disclosed on
request. A deadline is a goal date, never evidence of achieving the outcome.
The full-width muscle summary shows recorded-set distribution and selected-period
workouts and counted sets (the sets part is omitted at zero). Accepted performed sets retain their shares when required load
measurements are incomplete; `unmeasuredVolumeSets` discloses sets omitted from
physical volume, which is calculated only from measured sets. Missing groups are
not displayed as zero or assigned a training recommendation. The Home map uses
its compact centered 144×140 slot; muscle
details retain the full distribution and expose the PerGroup/PerMuscle selector
only when a known group distribution exists.
The rhythm summary counts distinct local dates with saved positive training
repetitions in the selected inclusive range, capped at the current time; exercise
catalog availability does not remove a performed day. Its target is an observed
rhythm from at least three fully observed completed weeks (including zero weeks),
not a profile/experience prescription. Supported cycles require at least three
repeats. Labels describe training days and observed rhythm, without probability
claims. Details show the last seven daily entries or four newest weekly/cycle
periods. Date labels scope the visible evidence; weekly/cycle rows also mark
partial observation. The rebuilt State retains the full timeline.
Performance compares the latest session with available nonnegative finite values
from preceding sessions in the selected period. Missing current/baseline values
remain unavailable; the displayed percentage and status share that comparison,
with a 5% minimum widened by historical variability. A period record requires a
strict increase over the preceding maximum. Duration remains neutral and retains
fractional minutes; its compact Home view deliberately shows the percentage
without a second average value. The five metrics retain their order and click
owners. Recorded zero remains available; a zero baseline has no percentage
comparison, and density requires a positive duration denominator. These pure
calculations add no telemetry or persisted fields.
Intensity uses saved Training.intensity in Home and PerformanceTrendDetails.
Newly saved totals retain measured zero-load repetitions in their average, so
increasing assistance alone cannot raise that average by removing a set. Existing
historical totals are not recalculated by these views. Exercise PeakIntensity is
a server-supplied achievement, not a client recomputation. The change adds no
Home analytics symbol or editable/archived metric state.
The last-training recap labels its mapped exercise count as Available exercises,
preserves saved duration/volume, identifies the selected-period scope and shows date,
duration, volume and exercise count; its existing Calendar action opens history.
Spotlights combine selected-period trainings with their observed exercise
catalog. The caller caps evaluatedAt at min(now, period.to) and supplies
period.from.date as the earliest queried history date. Progress and near-best
compare reliable set-derived workload (effective load × repetitions) within up to
90 days, carrying actual comparison counts/window days to UI. Missing catalog or
required load components suppress workload conclusions; recorded completed
repetitions can still support cadence. Frequency requires query bounds covering
28 days and reports recorded training days. Cache-first results and the existing
refresh/error path do not prove full server hydration. Sparse evidence omits the
corresponding card. The horizontal gallery retains complete localized insight,
context and explanatory text plus the existing exercise-details action. Cards
share a content-driven height and preserve scroll gutters; no callback or
persistence owner is added.
The spotlight's colored edge stripe is omitted; its localized semantic chip still
identifies the insight kind. Header chevrons add no callback or telemetry.
Welcome groups identity and artwork in one hero, then setup progress,
experience/facts and benefits. No additional
control callback, observation, analytics parameter, schema or screen identity is
introduced by these presentation choices.
Initial loading uses noninteractive Home skeleton cards with the shared shimmer;
no successful metric publication is represented by an empty performance list,
whereas a completed empty period contains five Empty-status metrics. An active
trainings refresh without period content also shows the skeleton; populated
cached cards stay visible. The existing period picker and its callback remain
available for returning users after range restoration, including during loading.
Skeleton slots add no event, operation, feature callback or persisted field,
and do not impose a minimum loading duration. Existing shared
error feedback still owns failures before the first successful metric publication.
The Progress hint is present only alongside a goal, muscle or streak summary;
it explains the existing card action and adds no control or observation.

| Observation owner / callback | Typed event / operation | Eligibility / wire payload |
|---|---|---|
| HomeViewModel.onOpenTrainings (LastTrainingCard, rendered only with a last training) | TrainingsClicked → `home_trainings_clicked` | Existing unconditional navigation; no parameters |
| onOpenExample (ExerciseSpotlightsCard) | ExerciseClicked → `home_exercise_clicked` | Existing dialog opening; exercise ID excluded |
| onOpenMuscleLoading / onOpenTrainingStreak / onOpenGoalDetails (GoalCard click) | MuscleLoadingClicked / TrainingStreakClicked / GoalDetailsClicked | Only after the nonnull resolved range guard |
| onPerformanceMetricClick(type) | PerformanceMetricClicked → `home_performance_metric_clicked` | Only after the nonnull range guard; metric values/type omitted; the performance cards render only past the lifetime unlock thresholds (two trainings for the duration trend, three for performance trends), so eligibility is also UI-gated |
| onOpenPeriodPicker (HighlightsHeader) | PeriodPickerClicked → `home_period_picker_clicked` | Existing dialog opening; no guard; the picker seeds from the current range kind |
| PeriodPicker `onResult` closure | PeriodConfirmed → `home_period_confirmed`, then SavePeriod | Accepted result delivered after the sheet finishes; the picker emits on every card tap, including the already selected kind; the complete `setRange(result)` block is decorated |
| onAddGoal (GoalCard update action and the HomeUnlockBanner "Set goal" call to action) and the TrainingGoalDetails `onAddGoal` closure | AddGoalClicked → `home_add_goal_clicked` | Three controls share one event; existing navigation at the callback owner; no reporting context propagated and no control attribution |
| onBack (BackCallback registered by HomeComponent) | DismissRequested → `home_dismiss_requested` | Existing Back callback |
| Range-driven `mapLatest` → `refreshTrainings`, a plain step under the Trainings loader | RefreshTrainings → `home_task_*` {home_operation = refresh_trainings} | Complete `getTrainings(start, end)` block; waits for the resolved persisted range. A failed request ends the period Flow collection: the error popup's Retry re-collects it for the current period under the Trainings loader, closing it stops later period refreshes for this ViewModel (Room observation keeps showing cached rows); no automatic retry. A replaced period or a lifecycle stop cancels the in-flight request with its `mapLatest` step. Clock-only local metric recomputation does not refresh the server, while changed rolling date bounds do; `Processing.WhileActive` re-runs it after a resume beyond the one-second disconnect grace |
| Fresh HomeViewModel `init` starts the catalog refresh, repeated only by the error popup's Retry | RefreshCatalogs → `home_task_*` {home_operation = refresh_catalogs} | Complete block in the existing VM-owned `safeLaunch`: `getMuscles()`, then `getEquipments()` (the required foreign-key parents of the examples), then `getExerciseExamples()`; a failure of any step fails the operation and Retry repeats all three. One startup operation per fresh VM. Ordinary background/foreground, tab reactivation and in-process locale changes do not repeat it. If `AppLocale.current()` changes between the request and the write (an in-app language switch, including a first explicit pin of the language already shown such as `en-US` → `en`, or a cold start that restored this route before the stored language was applied), `ExerciseExampleRepositoryImpl` discards the response with `CancellationException`: the operation reports `interrupted`, the error pipeline does not retry it, and the catalog keeps its previous rows until the next refresh. Root owns no catalog request; Room observations continue to rebuild Home data |
| onWelcomeConfettiShown | Deliberate omission | Rendering callback from the confetti parade, not a user click |

All ten interaction schemas are version 1 and parameterless; the operation schemas
`home_task_started` [home_operation] and `home_task_completed/failed/interrupted`
[home_operation, duration_ms] use the closed `home_operation` codes
`refresh_trainings`, `refresh_catalogs`, `save_period`. All ten HomeEvent
variants, the three HomeOperation values and the one HomeScreen value have VM
consumers and exhaustive encoders. Toolkit decoration owns fresh operation IDs,
monotonic timing, original return values/exceptions and cancellation; `trackEvent`
isolates ordinary reporting exceptions and the failed operation payload drops the
error object. Command exceptions reach the standard error pipeline. No entity IDs, selected date
bounds, raw input, measurements or errors are encoded.

`HomeAnalyticsModule` (`@Module @ComponentScan`) binds `HomeAnalyticsImpl` as
`HomeAnalytics` and registers one qualified `home_analytics` provider with the 14
schemas; `shared/.../Koin.kt` lists it once, and the module depends only on
`analytics-api`. Admin consumers of `home_operation` (the `Grippo Home operation`
dimension and diagnostic report selector) have historical reconciliation evidence
in the root GA4 review. Home owns its startup catalog operation under the
`refresh_catalogs` code. It replaced `refresh_exercise_examples` when the block grew
to refresh muscles and equipment before the examples, so earlier durations and
failures are not comparable; the typed catalog is test-build only, so no alias is
kept and only historical rows carry the old code. The live `Grippo Home operation`
and `События и экраны` descriptions were updated for the rename on 2026-10-28 (root
Home row).

Named rolling presets Last7/14/30/60/365Days include exactly N local calendar
dates: the start is N−1 dates before the anchor and both endpoints are inclusive.
The existing durable range kind is unchanged; date bounds rebuild through the
same range owner. `DateTimeUtils.trailingYear()` limitations retain their
anniversary bounds.
This correction changes queried bounds, without changing events, operation
decorators, tracking placement, schemas, consent or transport; admin synchronization
is not triggered.

RefreshTrainings is a plain `mapLatest` step: `refreshTrainings` runs its
`trackOperation` under the Trainings loader. The Home operation dimension, duration
metric, mobile diagnostics and dependent consumers were read back and retained
without writes; the current mapping and SDK delivery limits are in the root
[Home admin row](../COVERAGE.md#live-ga4-setup-review--2026-10-14). The complete
request block and its `refresh_trainings` code were unchanged by that revision. After a failed refresh, the
error popup's Retry re-collects the period subscription (a new operation); closing it
ends that subscription for this ViewModel, so later period changes create no refresh
operations. No automatic retry, success-rate claim or historical backfill is
introduced.

## Omissions and privacy

Dialog presentation/close/cancellation for the six overlays, metric type and
selected period attribution, control attribution for the three AddGoalClicked
emitters, the notification permission request and reminder scheduling, the
welcome-status read/write (read once with `first()`; the Celebrated write is
called directly inside the existing safeLaunch before showing confetti, so failures
reach the shared error pipeline) and the metric aggregation use cases are not tracked. Root forwarding
callbacks do not duplicate leaf events; the HomeRoot BackCallback closes without
DismissRequested.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| MainComponent / MainTabNavigation (host) | Active tab including `MainRouter.Home` | Preserve (host-owned) | `MainRouter.serializer()`, key `MainTabs` | Tab Back returns to Home through bringToFront; Home-tab Back falls through to close |
| HomeRootComponent | Child navigation | Preserve | `HomeRouter.serializer()`, key `HomeRootComponent` | Payload-free `HomeRouter.Home` only; depth always one; no route inputs or drafts |
| HomeRootViewModel | HomeRootState | Runtime only | Data object | Stateless container; no supplier |
| HomeViewModel | Accepted range kind | Preserve in the existing durable owner (no StateKeeper copy) | `LocalSettingsFeature.setRange/observeRange` → DataStore `range`; Range ↔ DateRangeKind mappers | Initial `DateRangeFormatState.Empty(kind = Last30Days)` with a null value until the store emits; an absent preference resolves to Last30Days only after that emission; `clearUserSettings` (logout/delete) removes `range`; the picker offers Last7/14/30/60, and `Infinity` is a resolvable persisted kind that yields Invalid with nonnull bounds |
| HomeViewModel | Range date bounds / formatter state | Rebuild | combine(LocalSettingsFeature.observeRange, currentDateFlow) → provideRange → DateRangeFormatState.of(kind) | Recomputed after the persisted kind emits and at each local midnight; selectedPeriods deduplicates date bounds for server refresh, metricPeriods adds date invalidation after distinct so local metrics also rebuild inside unchanged calendar bounds. Both retain the existing WhileActive lifecycle |
| HomeViewModel | `user` | Rebuild | `observeUser()` | Null emissions are ignored, so the user never clears once set |
| HomeViewModel | Catalog startup refresh | Runtime only / Rebuild | `init` → RefreshCatalogs (`MuscleFeature.getMuscles` → `EquipmentFeature.getEquipments` → `ExerciseExampleFeature.getExerciseExamples`) in one VM-owned block; catalog data stays in Room | A fresh retained VM runs the block once, including fresh construction after restoration. The error popup's Retry repeats the whole block, closing it does not, so a closed catalog failure starts no exercise-example request; a response discarded because the app language changed mid-request ends `interrupted` without a retry. Reusing the VM on foreground, tab activation or locale change adds no request; no catalog Job member, locale snapshot, refresh flag or draft exists in Home or Root |
| HomeViewModel | `lastTraining`, `performance` | Rebuild | Selected-period training observation → existing mappers / PerformanceTrendUseCase.fromTrainings | Empty period clears the last training and produces five unavailable metrics; fractional means remain read-only output |
| HomeViewModel | `streak` | Rebuild | `provideTrainings(list, period)` → TrainingStreakUseCase.fromTrainings(trainings = list, range = period) → mapper | Same observed period; now captured once and range capped before day/week/cycle calculation; empty period clears summary |
| HomeViewModel | `spotlights` | Rebuild | Selected-period trainings + observed relevant ExerciseExample catalog → ExerciseSpotlightUseCase.buildSpotlights(evaluatedAt = min(now, period.to), historyStart = period.from.date) → mapper | Rebuilds from the restored durable range and date invalidation/resume; empty period clears list; no draft or edit state |
| HomeViewModel | `goalProgress` (goal, optional score, evidence: GoalAnalysisEvidenceState(range, setCount, excludedSetCount, missingMetadataSetCount), derived analyzed days, findings, timeline) | Rebuild | metricPeriods (selected range + currentDateFlow) → combine(TrainingFeature.observeTrainings, GoalFeature.observeGoal), relevant exercise-catalog invalidation → GoalFollowingUseCase(range, goal, now) → mapper | Empty period preserves goal with unavailable score; removed goal clears card; latest source/range cancels prior calculation; no draft, secret or shadow cache |
| LastTrainingCard / GoalCard | Read-only workout creation and goal date labels | Rebuild in composition | `DateCompose` with the existing typed date and `DateTimeFormatState.format` | Current locale is resolved when rendering; absent typed values keep their existing display. No business dates or VM snapshots are changed |
| HomeViewModel | `muscleLoad`, including excluded/unassigned set counts and `unmeasuredVolumeSets` | Rebuild | Resolved range → observeTrainings(range) → flatMapLatest(MuscleLoadingSummaryUseCase.observeFromTrainings) → mapper | Training and referenced-anatomy changes recalculate the distribution; an empty training period publishes null. Calculations use available mapped exercise details; the map and goal card show only their conditional data notices (excluded/unassigned sets, limited data), without a completeness flag or inferred missing reps/weights. Accepted performed-set shares survive incomplete load measurements; the derived unmeasured count explains omitted physical volume. No editable or archived metric snapshot |
| HomeViewModel | `excludedMusclesCount`, `missingEquipmentCount`, `hasGoal` | Rebuild | Excluded-muscle/equipment and goal observations | Defaults 0/0/false |
| HomeViewModel | `showWelcomeConfetti` | Runtime only | `observeHomeWelcomeStatus().first()` → provideWelcomeStatus → `setHomeWelcomeStatus(Celebrated)`; DataStore `home_welcome_status`, default Idle | True once per fresh VM only after PendingCelebration is successfully persisted as Celebrated; failed writes use the shared error pipeline without displaying a successful celebration. Consumed inside WelcomeHomeContent and reset at animation start; an unfinished animation is not a draft |
| HomeViewModel / HomeScreen | Loading presentation | Runtime only | Existing range/user readiness, initial empty performance list, and Trainings loader without period content (or lifetime count <1) | Home skeleton until initial readiness; a real empty period has five Empty metrics and resolves after the loader. Populated cached content remains visible during refresh; the existing picker stays available for returning users after range restoration, even during loading. Shared error feedback remains the failure owner; no new loading flag/draft/timer |
| HomeViewModel | Jobs, whileActive subscriptions, notification permission/schedule, dialog sessions, analytics provenance | Runtime only | `Flow.safeLaunch` default WhileActive (one-second disconnect grace); each range refresh is a step of the range Flow; live DialogConfig callbacks; host serializers null | Resume beyond the grace re-emits the current range and repeats RefreshTrainings. A request failure ends the range refresh Flow unless the error popup's Retry re-collects it; range/lifecycle cancellation cancels the in-flight request with its `mapLatest` step. NonCancellable is confined to cancellation cleanup; requests remain cancellable. No persisted Job, restoration flag, previous-source cache or shadow copy |
| Home dialogs | Unaccepted picker selection; `onResult`/`onAddGoal` closures | Runtime only | Dialog slot/step stacks unserialized; the picker value lives only in the dialog VM | The accepted period goes straight to DataStore through SavePeriod; a result still queued at process death has no durability |
| Home composables | Dashboard and spotlight-gallery scroll positions | Presentation only | Existing anchored grid state; gallery's standard `rememberScrollState` pixel offset | No business selection, draft field, restoration flag or ViewModel owner added; swipe scrolling has no feature analytics event |

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
those revisions. The catalog revision (startup operation renamed to
`refresh_catalogs`, muscles and equipment refreshed inside it) passed
`./gradlew :androidApp:assembleDebug` and
`./gradlew :shared:assembleSharedDebugXCFramework`; runtime checks were not run.

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
