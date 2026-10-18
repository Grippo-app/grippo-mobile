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

BackHandler invokes `onBack`. `DismissRequested` records a request, not completed
dismissal. Swipe/outside dismissal can bypass the VM and remains omitted. Dialog
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
| TrainingGoalDetailsViewModel | progress; presentation insights derive from it | Rebuild | Combined TrainingFeature.observeTrainings, GoalFeature.observeGoal and currentDateFlow; relevant ExerciseExampleFeature.observeExerciseExamples IDs trigger recalculation; GoalFollowingUseCase with explicit range/current goal/current time | Fresh VM starts with null progress and derived NoData insights; goal removal clears progress; an existing goal with empty period retains its card and a null score; latest source updates cancel obsolete computation |
| GoalProgressState in dialog | grouped GoalAnalysisEvidenceState (range and valid/excluded/metadata-missing set counts), derived days/timeline, findings and prepared optional domain score | Rebuild | Domain adherence mapped through domain-to-state | Only observations since goal creation and inside the selected range are represented; insufficient observations have no numeric score |
| Rendered content | Localized dates, evidence, reasons, tips and goal presentation | Rebuild | Typed date endpoints rendered with DateCompose; current resources and rebuilt progress | Locale changes resolve period endpoints and copy again; no resolved-label cache or saved business snapshot |
| Dialog host | Session, callbacks, pending directions | Runtime only | DialogComponent slot and DialogContentComponent stack use serializer = null | Process death closes the dialog; callbacks are never archived |
| Foundation / widgets | Jobs, loaders, scroll state and telemetry provenance | Runtime only | Existing foundation/widget ownership | No new persistence owner or synthetic interaction replay |

The sheet owns no editable fields, selections, accepted picker results or drafts.
Range is a caller seed. Data-backed fields rebuild reactively; they are not saved.
There are no secrets or private initialization/restoration flags, previous-source
caches or shadow snapshots. Existing Back and parent navigation ownership is
unchanged. Parent Home drafts and shell restoration retain their own inventories.

## Calculation presentation

The score summary follows the optional domain score. A null score shows TooEarly
only when valid sets exist in a period shorter than `MIN_ANALYSIS_DAYS`; otherwise
it shows NoData. Numeric summaries use the shared score thresholds. Diagnostic
cards render the same findings that supply the score. The sheet does not infer
share-based training prescriptions, overtraining, achieved outcomes or completion
from a deadline. Overdue/almost-due timeline notes are independent of score.

A static localized caption scopes the score, evidence and findings to available
mapped exercise details; it does not claim complete history or retain a
completeness flag. Analyzed days and goal timeline values derive from their
existing dates rather than independent constructor fields.
Evidence displays the actual analysis period, analyzed days, counted sets and
conditional excluded/metadata-missing set counts. Method copy explains the
heuristic scope. The goal remains editable through its existing card action.

## Admin synchronization impact

The behavior change alters no event/operation model, parameter, schema, encoder,
screen enum/hook, tracking call type/arguments/placement, consent or transport.
Both direct callback tracking calls remain before their original directions.
Changes to local calculation, rendered evidence and rebuilt state introduce no
analytics-facing symbol change, so the admin-sync trigger does not apply. Live
admin configuration and SDK delivery are not established by this source inventory.
