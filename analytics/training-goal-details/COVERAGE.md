# Training goal details coverage

Scope: `:ui-dialog-features:training-goal-details` and its Home and Completed
entry points.

## Surface and observation inventory

`HomeViewModel.onOpenGoalDetails` opens `DialogConfig.TrainingGoalDetails` after
its valid-range guard. `TrainingCompletedViewModel.onGoalClick` (the Completed
screen's Growth GoalCard) opens the same config with the Completed period after its
resolved-period guard; its `onAddGoal` closure emits `AddGoalClicked` and then
the `TrainingCompletedDirection.Goal` direction. `DialogContentComponent` constructs
`TrainingGoalDetailsComponent`, whose retained `TrainingGoalDetailsViewModel`
renders `TrainingGoalDetailsScreen`. The sole surface is a read-only bottom
sheet. It has no nested flow or independent screen, so its default null
`screenPresentation` is appropriate. Dialogs have no automatic view/close schema.
No screen enum or separate dialog presentation contract is introduced.


| Route / rendered surface | Component → ViewModel | Screen hook or dialog reason |
|---|---|---|
| `DialogConfig.TrainingGoalDetails` → `TrainingGoalDetailsScreen` | TrainingGoalDetailsComponent → TrainingGoalDetailsViewModel | Default null: in-memory dialog outside independent-screen presentation policy |
| `HomeRouter.Home` → DashboardHomeContent GoalCard entry | HomeComponent → HomeViewModel | Existing typed `HomeScreen.Home` hook; full Home variants and containers are inventoried in `analytics/home/COVERAGE.md` |
| `TrainingRouter.Completed` → Growth GoalCard entry | TrainingCompletedComponent → TrainingCompletedViewModel | Existing typed `TrainingScreen.Completed` hook; the Completed screen and its container are inventoried in `analytics/training/COVERAGE.md` |

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| TrainingGoalDetailsViewModel.onBack | DismissRequested / training_goal_details_dismiss_requested v1 | Existing callback without guards; empty payload before Back direction |
| TrainingGoalDetailsViewModel.onAddGoal | AddGoalClicked / training_goal_details_add_goal v1 | Existing callback without guards; Overdue calendar Update click before AddGoal direction; empty payload |
| HomeViewModel.onOpenGoalDetails | HomeEvent.GoalDetailsClicked | Existing valid-range guard; parent-owned open intent |
| HomeViewModel config onAddGoal closure | HomeEvent.AddGoalClicked | Parent observes its accepted navigation callback, separate from dialog click intent |
| TrainingCompletedViewModel.onGoalClick | TrainingEvent.Completed.GoalOpened / completed_goal_opened v1 | Resolved-period guard (`state.range.value`); parent-owned open intent with title `goal_details_title` and the Completed period; empty payload |
| TrainingCompletedViewModel config onAddGoal closure | TrainingEvent.Completed.AddGoalClicked / completed_add_goal_clicked v1 | Parent observes its accepted navigation callback before `TrainingCompletedDirection.Goal`; the Growth GoalCard itself has no add/update action |
| Training/goal/catalog observation and adherence calculation | Deliberate omission | Reactive presentation rebuilding, no finite command or submission |
| Local assessment disclosure | Deliberate omission | Presentation-only expand/collapse; no domain command, navigation or submission |

The leaf `onBack` callback records `DismissRequested`, not completed dismissal.
The shared host handles toolbar Back/Close, Compose/system Back and swipe/outside
dismissal without invoking the leaf callback; those paths remain omitted. Dialog
views/close, scrolling, calculation/card impressions and navigation completion are
omitted. There are no editable inputs or picker-result callbacks. No operation API
is introduced for continuous observation or calculation. Events carry no goals,
scores, dates/range, training IDs, raw text, measurements or errors. No reporting
context is transported through routes.

The stateless typed service is injected inside the normal retained VM factory.
Its exhaustive matrix encodes two parameterless v1 schemas; the annotated module
registers its qualified catalog in shared. Reporting isolation and both existing
navigation directions retain their current owners. The two caller events belong to
`HomeAnalytics` and `TrainingAnalytics` respectively; this module encodes only the
sheet's own two facts.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| TrainingGoalDetailsComponent / VM | Immutable range seed | Runtime only | Caller range (Home's selected range or Completed's resolved period), retainedInstance | Reopening receives the caller's current range; retained recreation reuses the VM |
| TrainingGoalDetailsViewModel | progress; priority, positive and additional findings plus unavailable-assessment copy derive from it | Rebuild | Combined TrainingFeature.observeTrainings, GoalFeature.observeGoal and currentDateFlow; relevant ExerciseExampleFeature.observeExerciseExamples IDs trigger recalculation; GoalFollowingUseCase with explicit range/current goal/current time | Fresh VM starts with null progress and a neutral NoData explanation; goal removal clears progress; an existing goal with empty period retains its summary and a null score; latest source updates cancel obsolete computation |
| GoalProgressState in dialog | grouped GoalAnalysisEvidenceState (range and valid/excluded/metadata-missing set counts), derived days/timeline, findings and prepared optional domain score | Rebuild | Domain adherence mapped through domain-to-state | Only observations since goal creation and inside the selected range are represented; insufficient observations have no numeric score |
| Rendered content | Localized dates, evidence, unavailable-assessment copy and goal presentation | Rebuild | Typed date range rendered through DateRangeFormatState.short(); current resources and rebuilt progress | Locale changes resolve period endpoints and copy again; no resolved-label cache or saved business snapshot |
| TrainingGoalDetailsScreen | Assessment disclosure | Runtime only | Local rememberSaveable keyed by goal primary/creation date | Collapsed for a fresh goal/session; survives recomposition/configuration within the current in-memory dialog; no process-death archive |
| Dialog host | Session, callbacks, pending directions | Runtime only | DialogComponent slot and DialogContentComponent stack use serializer = null | Process death closes the dialog; callbacks are never archived |
| Foundation / widgets | Jobs, loaders, scroll state and telemetry provenance | Runtime only | Existing foundation/widget ownership | No new persistence owner or synthetic interaction replay |

The sheet owns no editable fields, selections, accepted picker results or drafts.
Range is a caller seed. Data-backed fields rebuild reactively; they are not saved.
There are no secrets or private initialization/restoration flags, previous-source
caches or shadow snapshots. Existing Back and parent navigation ownership is
unchanged. Parent Home and Completed drafts and shell restoration retain their own
inventories.

## Calculation presentation

The dialog uses the shared GoalCard Assessment variant with GoalMark, primary goal,
optional domain score and semantic status. Its modest neutral percentage is
explicitly labelled training alignment. The Home Overview card remains
unchanged. A null score has no corrective or positive diagnostic findings; a
neutral notice distinguishes TooEarly, NoData and no analysis-range overlap.
Null progress retains the existing neutral unavailable-assessment notice and
never claims a confirmed missing goal or exposes a premature goal-creation action.

The sheet keeps at most two priority non-PASS findings visible in the existing
severity/rule order, followed by one PASS finding when available. This positive
fact acknowledges recorded evidence without inventing praise or coaching.
Disclosure expands and fades in the remaining scored findings and methodology
without replacing the visible findings or control. Visible findings are excluded
from disclosure, so each finding appears once. Actual analysis dates, eligible
session count, analyzed days and counted sets appear in the summary; conditional
excluded and metadata-missing set counts remain in the disclosure. Findings keep
their existing units, bounds and formatting, including rounded threshold qualifiers.
The shared diagnostic block presents every fact on the same reading edge: rule,
optional muscle context, actual value, then a separate localized target line.
It uses one quiet section surface with dividers between facts; no nested context
indentation or combined actual/target sentence remains. No priority formula,
prescription or business scoring changes.

Partial-data limitations remain visible with an available score. A quiet calendar
section follows the diagnostic findings, keeping deadline/remaining time separate
from training alignment. Expiry never implies achievement. The shared GoalCard Calendar variant retains the overdue
Update action that opens the goal editor. Reactive sources, domain scoring,
navigation owners and disclosure lifetime are unchanged.

## Admin synchronization impact

The behavior change alters no event/operation model, parameter, schema, encoder,
screen enum/hook, tracking call type/arguments/placement, consent or transport.
Both direct callback tracking calls remain before their original directions.
Changes to local calculation, rendered evidence and rebuilt state introduce no
analytics-facing symbol change, so the admin-sync trigger does not apply. Live
admin configuration and SDK delivery are not established by this source inventory.
