# Training goal details coverage

Scope: `:ui-dialog-features:training-goal-details` and its Home entry point.

## Surface and observation inventory

`HomeViewModel.onOpenGoalDetails` opens `DialogConfig.TrainingGoalDetails` after
its valid-range guard. `DialogContentComponent` constructs
`TrainingGoalDetailsComponent`, whose retained `TrainingGoalDetailsViewModel`
renders `TrainingGoalDetailsScreen`. The sole surface is a read-only bottom
sheet. It has no nested flow or independent screen, so its default null
`screenPresentation` is appropriate. Dialogs have no automatic view/close schema.
No screen enum or separate dialog presentation contract is introduced.

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| TrainingGoalDetailsViewModel.onBack | DismissRequested / training_goal_details_dismiss_requested v1 | Existing callback without guards; empty payload before Back direction |
| TrainingGoalDetailsViewModel.onAddGoal | AddGoalClicked / training_goal_details_add_goal v1 | Existing callback without guards; GoalCard update click before AddGoal direction; empty payload |
| HomeViewModel.onOpenGoalDetails | HomeEvent.GoalDetailsClicked | Existing valid-range guard; parent-owned open intent |
| HomeViewModel config onAddGoal closure | HomeEvent.AddGoalClicked | Parent observes its accepted navigation callback, separate from dialog click intent |
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
navigation directions retain their current owners.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| TrainingGoalDetailsComponent / VM | Immutable range seed | Runtime only | Caller range, retainedInstance | Reopening receives the current caller range; retained recreation reuses the VM |
| TrainingGoalDetailsViewModel | progress; attention findings and unavailable-assessment copy derive from it | Rebuild | Combined TrainingFeature.observeTrainings, GoalFeature.observeGoal and currentDateFlow; relevant ExerciseExampleFeature.observeExerciseExamples IDs trigger recalculation; GoalFollowingUseCase with explicit range/current goal/current time | Fresh VM starts with null progress and a neutral NoData explanation; goal removal clears progress; an existing goal with empty period retains its card and a null score; latest source updates cancel obsolete computation |
| GoalProgressState in dialog | grouped GoalAnalysisEvidenceState (range and valid/excluded/metadata-missing set counts), derived days/timeline, findings and prepared optional domain score | Rebuild | Domain adherence mapped through domain-to-state | Only observations since goal creation and inside the selected range are represented; insufficient observations have no numeric score |
| Rendered content | Localized dates, evidence, unavailable-assessment copy and goal presentation | Rebuild | Typed date endpoints rendered with DateCompose; current resources and rebuilt progress | Locale changes resolve period endpoints and copy again; no resolved-label cache or saved business snapshot |
| TrainingGoalDetailsScreen | Assessment disclosure | Runtime only | Local rememberSaveable keyed by goal primary/creation date | Collapsed for a fresh goal/session; survives recomposition/configuration within the current in-memory dialog; no process-death archive |
| Dialog host | Session, callbacks, pending directions | Runtime only | DialogComponent slot and DialogContentComponent stack use serializer = null | Process death closes the dialog; callbacks are never archived |
| Foundation / widgets | Jobs, loaders, scroll state and telemetry provenance | Runtime only | Existing foundation/widget ownership | No new persistence owner or synthetic interaction replay |

The sheet owns no editable fields, selections, accepted picker results or drafts.
Range is a caller seed. Data-backed fields rebuild reactively; they are not saved.
There are no secrets or private initialization/restoration flags, previous-source
caches or shadow snapshots. Existing Back and parent navigation ownership is
unchanged. Parent Home drafts and shell restoration retain their own inventories.

## Calculation presentation

The shared GoalCard renders the optional domain score and its status once. A
null score shows TooEarly only when valid sets exist in a period shorter than
`MIN_ANALYSIS_DAYS`; otherwise it shows NoData, or the existing no-overlap
explanation for an unavailable analysis range. No corrective findings are
presented as an evaluated assessment without a numeric score.

The collapsed sheet shows at most two non-PASS findings in the existing
severity/rule order. Disclosure exposes all scored non-PASS and PASS findings,
actual analysis dates, analyzed days, counted sets and conditional excluded or
metadata-missing set counts. Findings preserve their existing units, bounds and
formatting. No new priority formula, prescription or coaching is calculated.

The analyzed period and short method distinguish training alignment from goal
achievement. The shared card keeps its partial-data notice. Deadline and remaining
time retain their independent calendar meaning; no achievement or completion is
inferred from expiry. The existing overdue Update action still opens the goal
editor. Source observations, domain scoring and navigation owners are unchanged.

## Admin synchronization impact

The behavior change alters no event/operation model, parameter, schema, encoder,
screen enum/hook, tracking call type/arguments/placement, consent or transport.
Both direct callback tracking calls remain before their original directions.
Changes to local calculation, rendered evidence and rebuilt state introduce no
analytics-facing symbol change, so the admin-sync trigger does not apply. Live
admin configuration and SDK delivery are not established by this source inventory.
