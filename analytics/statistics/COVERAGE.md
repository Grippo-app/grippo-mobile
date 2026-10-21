## Current contract follow-up (2026-10-14)

The [current analytics review](../COVERAGE.md#current-analytics-contract-review-2026-10-14)
records this revision and its verification limits. Typed dialog observation names
use DismissRequested across screens/dialogs, with dismiss_requested wire names/codes.
The app has not been released; all analytics schemas use initial version 1.
This feature's operation schemas use version 1 and omit visible-screen context;
feature operation codes and operation_id own outcome correlation. Earlier build
statements below describe their original source revisions, not released contracts.

# Statistics coverage

Scope: `:ui-dialog-features:statistics`, 2026-10-13.

## Surface gate

Both `DialogConfig.Statistics.Trainings` (TrainingsViewModel.onDailyDigestViewStats)
and `DialogConfig.Statistics.Training` (TrainingsViewModel.onTrainingMenuClick → Details result → openTrainingOverview
and TrainingCompletedViewModel.onSummaryClick) reach the shared
DialogContentComponent factory → StatisticsComponent → retained
StatisticsViewModel → StatisticsScreen. The modes share one read-only bottom sheet;
there are no nested or independent screens. The VM keeps default null
screenPresentation under the dialog policy. No dedicated presentation contract,
screen enum, view/close events or feature lifecycle wiring is introduced.

## Observation inventory

| Owner / path | Observation | Payload / eligibility |
|---|---|---|
| StatisticsViewModel.onBack / registered BackCallback | StatisticsEvent.DismissRequested | statistics_dismiss_requested v1; empty payload, before existing Back direction; no guard |
| Trainings mode startup safeLaunch / Charts loader | StatisticsOperation.RefreshTrainings | statistics_task_started/completed/failed/interrupted v1; statistics_operation=refresh_trainings, terminal duration_ms; foundation supplies fresh operation identity |
| Training mode observeTraining | Deliberate omission | Reactive source observation; no finite request |
| observeTrainings and metric calculations | Deliberate omission | Reactive presentation rebuilding and local calculations |
| Parent open callbacks | Existing parent analytics, unchanged | Remain owned by Trainings/Training; no context transported into this dialog |

DismissRequested describes the observed request, not delivered dismissal or
cancellation. The shared Compose BackHandler calls DialogContract.onDismiss directly;
the toolbar Back and Close callbacks are also host-owned. These paths, as well as
swipe/outside dismissal, bypass StatisticsViewModel.onBack and are omitted. Only
an actual invocation of the feature callback (including its Essenty BackCallback
when reached) emits this event; there is no claim of complete Back/close coverage.
No shared host/config reporting context or wiring is added. Scrolling,
chart gestures and automatic dialog view/close telemetry are omitted. No synthetic
callback events occur during initialization/recreation. The complete existing
getTrainings(...) stays inside the decorator, in its existing coroutine
and loader. It preserves results, business errors and cancellation through the
existing safeLaunch pipeline. Reporting Exceptions are isolated by the service
and canonical toolkit operation helper; fatal Errors remain fatal.

The stateless service is resolved inside the retained VM factory. UI depends only
on analytics-api. The internal implementation/module and qualified
statistics_analytics catalog are explicitly registered in settings and shared DI.
No dates, ranges, training IDs, counts of exercises, metric/body values, raw text,
error messages or request/response data enter the payload.

## State preservation gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| StatisticsViewModel | mode / configured range or training ID | Runtime only | Existing DialogConfig inputs | Live dialog session; no editable filters or accepted results |
| StatisticsViewModel | total, exerciseVolume, muscleLoad | Rebuild | observeTrainings/observeTraining plus metric use cases; muscleLoad also observes referenced anatomy through observeFromTrainings/observeFromExercises | Both modes calculate from available mapped exercise details; no scope caption is rendered and no completeness state is inferred. Anatomy changes recalculate shares and excluded/unassigned notices |
| StatisticsViewModel | Charts loader, jobs, Back direction, telemetry | Runtime only | Foundation runtime owners | No archive supplier or serializer |
| StatisticsComponent | ViewModel | Runtime retention | Existing retainedInstance | Ordinary configuration recreation retains VM; process restart closes dialog |
| StatisticsScreen | Configured range label | Rebuild in composition | `DateRangeFormatState.short()` from the existing mode range | Uses the current locale without changing the range, VM state, inputs or dialog lifetime |

No user-owned draft, selection or parent-flow output exists in this module.
Therefore no StateKeeper key, initialState boundary or serialization is required.
Both modes calculate from available mapped exercise details without a visible
scope caption; a detail-derived zero describes that available sample, rather
than the entire saved history. Independent saved
training aggregates retain their existing owners. No completeness flag, extra
draft, retry, cache or operation is added.
Both shared dialog hosts use serializer=null intentionally. No dialog sessions,
callbacks, metrics, secrets or telemetry provenance are archived. Opening the sheet
again supplies business inputs and recomputes data; it does not replay user actions.
The shared volume bar chart derives its geometry, exact formatted values and
accessibility description from the existing series. Tap selection and horizontal
exploration are local presentation state. They add no persisted field, route,
Contract callback, analytics symbol, operation, consent or transport change.
Daily training buckets use localized day-and-month labels instead of repeated
weekday names. The existing pure series producer retains bucket selection,
chronological order, totals, observation and request boundaries.

## Verification

Source/diff review covers both entry modes, rendered content, Back wiring, retained
injection, DI/schema registration, exact payloads, complete operation boundaries,
original exception/cancellation propagation and reporting isolation. Screen and
state source gates pass by the inventories above. No automated tests/infrastructure
were added under the project verification policy.

The earlier analytics verification did not exercise both modes with populated/empty sources, Back versus
swipe dismissal, successful/failed/cancelled requests, reporting-failure injection,
pause/resume subscriptions, retained Android recreation and fresh Android/iOS
process restart (the dialog must remain closed). Compilation cannot prove these
checks or Firebase delivery. Build results are recorded below.

`git diff --check` passed. Combined scoped build passed (exit 0):
`:analytics:statistics:compileAndroidMain`,
`:ui-dialog-features:statistics:compileAndroidMain`,
`:analytics:statistics:compileKotlinIosSimulatorArm64`,
`:ui-dialog-features:statistics:compileKotlinIosSimulatorArm64`.
Generated Koin sources were inspected for StatisticsAnalytics binding and the
qualified schema catalog. Existing global opt-in warnings remain.

An earlier combined shared metadata check failed in ui-core:foundation iOS metadata
because a toolkit/logger intermediate klib was absent during concurrent builds;
no unrelated infrastructure was changed. An initial configuration attempt also
encountered an unregistered parallel weight-picker module before its registration
appeared. These failures do not establish a defect in statistics.

The subsequent standalone `:shared:kspCommonMainKotlinMetadata` check passed
(exit 0), after the scoped compilation rebuilt the dependencies. No infrastructure
or unrelated source changes were needed. This confirms generated shared metadata,
not a full application/native framework build or runtime DI resolution.

## Follow-up source review

Rechecked both config entry modes, retained injection, module/dependency registration,
all five versioned schemas, payload privacy, complete refresh block, cancellation
and reporting isolation against current source. No scoped production defect was
found. Corrected the omission inventory to include host Compose Back and toolbar
Close/Back paths; those callbacks do not invoke the statistics VM. StateKeeper
remains unnecessary because there is no editable draft or accepted output.

Those earlier analytics revisions had no independent review. The current digest
consumer changes have independent code, design and analytics/preservation reviews;
source acceptance does not establish delivery, restoration or failure-injection
behavior. Earlier scoped build results above remain historical evidence.
