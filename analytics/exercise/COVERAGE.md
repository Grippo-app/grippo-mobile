# Exercise dialog coverage

Scope: `:ui-dialog-features:exercise`, the read-only record opened from a
completed exercise in workout history or the completed-workout screen.

## Surface and presentation gate

| Route / rendered surface | Component → VM | Presentation classification |
|---|---|---|
| `DialogConfig.Exercise` → dialog content child → `ExerciseScreen` | ExerciseComponent → ExerciseViewModel | Read-only runtime overlay; default null `screenPresentation`. Loading, unavailable and available content are states of the same overlay, without a separate presentation contract. |
| `TrainingsRouter.Trainings` → daily timeline → Exercise overlay | TrainingsComponent → TrainingsViewModel | Existing `TrainingsScreen.Calendar` presentation and `TrainingsEvent.ExerciseClicked` open intent remain unchanged. |
| `TrainingRouter.Completed` → completed timeline → Exercise overlay | TrainingCompletedComponent → TrainingCompletedViewModel | Existing `TrainingScreen.Completed` presentation and `TrainingEvent.Completed.ExerciseOpened` open intent remain unchanged. |
| `ShowExampleDetails` → host pushes `DialogConfig.ExerciseExampleDetails` | Existing ExerciseExampleDetailsComponent → VM | Existing nested runtime overlay; no new navigation or analytics contract. |

Both entries use the same host factory. ExerciseComponent resolves
ExerciseAnalytics inside `retainedInstance`. Shared registers ExerciseAnalyticsModule
and its qualified `exercise_analytics` schema. The host retains its toolbar,
Back/close behavior and in-memory session ownership.

## Observed callbacks, operation and schemas

| Owner / callback | Typed fact / version-1 wire schema | Eligibility and meaning |
|---|---|---|
| ExerciseViewModel.onExampleDetailsClick | ExampleDetailsClicked / `exercise_example_details_clicked` | After the existing available exercise/example ID guard, before ShowExampleDetails; observes the click, not downstream display. Loading/unavailable content emits nothing. |
| ExerciseViewModel.onDismiss | DismissRequested / `exercise_dismiss_requested` | Before Back; reports callback invocation without inferring user source or host dismissal. |
| ExerciseViewModel.init / startup safeLaunch | LoadHistory / `exercise_task_started`, `exercise_task_completed`, `exercise_task_failed`, `exercise_task_interrupted` | Once after the primary subscription supplies an available record. The complete decorated block rechecks the selected record, refreshes the 180-day workout window, reads the reconciled cache and fresh selected record, selects prior workouts and maps their history. `getOrThrow()` remains inside the block. |

The two existing event schemas remain parameterless. The four version-1
operation schemas carry `exercise_operation=load_history`; terminal schemas also
carry numeric `duration_ms`. No IDs, names, URLs, dates, measurements, set values
or exception text are reported. Completion describes the whole read/reconciliation
block, including an eligible early return if the selected record disappears;
it does not prove that a prior workout exists or that a comparison was rendered.
The sets, comparison and chart add no business commands. `observeExercise`
remains a subscription, not an operation. Chart tooltips are local presentation
interactions and deliberately emit no analytics.
The shared bar chart exposes exact recorded values with units on tap and keeps
the complete series accessible. Chart selection is local presentation state;
its visual style, tooltip formatting and horizontal exploration introduce no
Contract callback, business command, typed event or transport change.

Omissions: automatic dialog view/close, host Back/swipe/outside-tap dismissal,
programmatic removal, entry attribution and nested-example interaction/outcome.
Host exits can bypass `onDismiss`. No synthetic events on initialization, content
emissions or resume. The existing detail action provides exercise information,
recent history and achievements through their existing owners.

## State preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| ExerciseViewModel / ExerciseState | Available exercise: identity, date, ordered iterations and stored totals | Rebuild | TrainingFeature.observeExercise(id) → domain-to-state mapper | The primary subscription alone owns current-record state. Fresh session starts Loading; nonnull source becomes Available. A null source becomes Unavailable, clearing any earlier available record. No editable data. |
| ExerciseState.Available.history | Immutable prior exercise records | Rebuild | TrainingFeature.getTrainings plus observeTrainings, SelectExerciseHistoryUseCase and domain-to-state mapper | The selected date bounds a 180-day window through the end of that day. At most five strictly prior workouts contribute one recorded matching exercise each; the current workout and future records are excluded. Same-ID primary emissions preserve history data. Progress belongs only to ExerciseLoader.History in the existing safeLaunch; State stores no duplicate loading mode. There is no draft or persistent comparison cache. A partial prior record remains the actual latest record; unknown load is omitted from comparisons rather than replaced by an older session. |
| ExerciseState | Loading / Available / Unavailable content lifecycle | Rebuild | Initial state and the current observeExercise emission | No saved flags, source snapshots or duplicated draft. An unavailable record exposes no details action. |
| ExerciseScreen | Scroll position | Runtime only | Existing Compose scroll owner | Presentation-only; no business selection or accepted result. |
| ExerciseVolumeHistoryChart | Tooltip / touched bar | Runtime only | Existing shared BarChart owner | No business selection; recreated from chronological history with the selected entry last. |
| DialogConfig.Exercise / host | id, title, callbacks and session identity | Runtime only | Existing slot and content stack, serializer = null | Retained VM supports ordinary recreation; process death closes the dialog session. No StateKeeper supplier is added. |
| Exercise VM / foundation | loaders, directions and subscription | Runtime only | Existing foundation and retainedInstance | Jobs, callbacks and reporting metadata are not serialized. |
| Calling screens | Existing date/range and completed-workout draft | Preserve | Existing `TrainingsDraft` / `TrainingCompletedDraft` Component StateKeeper keys | Unchanged. This dialog returns no editable selection/result; detail navigation uses the existing example ID. |

No new serialization, routes, archive schema or app-shell changes. The module
declares the existing date-utils and datetime dependencies directly for the
production history window and deterministic date fixtures; versions and build
conventions are unchanged.
Stored totals keep their current domain meaning and originate in nonnullable
Float/Int domain fields. Their formatter-empty zero values render as recorded
zero in this feature summary; absent per-set measurements remain unavailable.
Bodyweight formulas use the recorded component values. Optional absent body mass
contributes zero while known external, extra and assist values remain recorded
load. Body mass without its multiplier and a coefficient without any weight input
remain unknown. The sets section reuses IterationCard.Recorded and pairs matching load bases by
ordinal in a quiet previous-result column; it does not infer set equivalence or
strength progress. Comparison data, loading and accessibility stay in the feature-owned trailing slot, outside the IterationCard entity content. Nullable recorded-load calculation and comparison bases belong to the existing reusable IterationState/ExerciseState models. The fixed context width and two-line minimum reserve the same
space for shimmer, prior values and absent matches.

The compact summary after the sets shows only current volume, repetitions and
maximum effective load; set count already belongs to the Sets heading.
Secondary history shows a concise previous value/date and guarded volume percent.
The percentage requires a matching latest-prior load basis, a positive previous
volume and a finite bounded ratio. The chart requires comparable load bases and
finite nonnegative volumes for every displayed entry; otherwise explanatory copy
occupies its reserved region. Signed recorded values remain visible in the sets,
current summary and previous caption without clamping them to a fabricated zero.
An incomparable latest-prior record also suppresses the percentage. History loading has one
localized indeterminate accessibility node, decorative shared shimmer slots and
the same caption/chart geometry as loaded or empty history. It introduces no
draft, timer, request, retry or business selection.

History refresh failures use the standard `safeLaunch` / ErrorProvider pipeline.
The user explicitly kept the general error dialog after being informed that the
host replaces and closes the Exercise session. There is no local retry/failure
state or retry event; re-entry rebuilds the record and history. Cancellation uses
the standard interrupted operation outcome, without a synthetic failure UI.

## Admin synchronization

The history operation changes analytics-facing symbols, so the live admin gate
applies. Live inspection in the user's correct Chrome window verified account
`379805710`, property `519002689` and both configured mobile app streams:
Android `13265245917` / `com.grippo.android`, iOS `13348167981` / `com.grippo.app`.
Their Firebase app identifiers match the synced public configurations.

Keep raw event-name diagnostics in saved report `События и экраны`
(`16123956244`): Event name is the default dimension, Event count and Total users
are the metrics, and its only filter is mobile Platform. It has no event allowlist,
so all six exercise schemas are eligible without an admin addition when delivered.
The registered `duration_ms` metric retains Event scope and Milliseconds; it is
available for explicit mobile analysis of the exact terminal exercise family
`^exercise_task_(completed|failed|interrupted)$`, but this saved diagnostics report
does not display duration and no Exercise latency report is claimed.
`exercise_operation` is not a registered dimension: the only current operation is
already identified by its unique event family, so a new dimension is deliberately
omitted and must be reconsidered if that family gains other operations. No derived
rule or key event is needed. Both mobile streams' exact profile/training derived rules, existing key
events, audiences, the profile/training report and training exploration/funnel have
no Exercise dependency. No admin configuration change was required or made.

Current-build SDK receipt and processing remain unobserved; configuration
reconciliation does not prove delivery. Root consumer mapping and evidence:
[`analytics/COVERAGE.md`](../COVERAGE.md).

## Verification boundary

The source inventory includes both entry points, the shared host, Component,
State, ViewModel, ordered set rendering and both callback guards. Runtime and
build results belong in the task reply; source review does not establish SDK
receipt, lifecycle resubscription or restoration. No verification-only production
hooks or automated test infrastructure are introduced.

Typed events, parameters, eligibility, consent and transport retain the recorded
contract. The startup decorator remains after the available-record gate within
the same initialization path; Available stores an immutable history list, while ExerciseLoader.History supplies
the only operation progress. Empty comparison and incompatible load data use a
neutral BannerCard in the existing reserved plot region. The admin
section retains its live configuration evidence; layout and chart eligibility
changes add no analytics-facing symbols or admin consumer dependency.
