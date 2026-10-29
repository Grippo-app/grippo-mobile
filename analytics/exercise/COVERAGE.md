# Exercise dialog coverage

Scope: `:ui-dialog-features:exercise`, the read-only record opened from a
completed exercise in workout history.

## Surface and presentation gate

| Route / rendered surface | Component → VM | Presentation classification |
|---|---|---|
| `DialogConfig.Exercise` → dialog content child → `ExerciseScreen` | ExerciseComponent → ExerciseViewModel | Read-only runtime overlay; default null `screenPresentation`. Loading, unavailable and available content are states of the same overlay, without a separate presentation contract. |
| `TrainingsRouter.Trainings` → daily timeline → Exercise overlay | TrainingsComponent → TrainingsViewModel | Existing `TrainingsScreen.Calendar` presentation and `TrainingsEvent.ExerciseClicked` open intent remain unchanged. |
| `ShowExampleDetails` → host pushes `DialogConfig.ExerciseExampleDetails` | Existing ExerciseExampleDetailsComponent → VM | Existing nested runtime overlay; no new navigation or analytics contract. |

The trainings calendar's daily timeline is the only caller (`TrainingsViewModel` is the
only `DialogConfig.Exercise` opener); it uses the shared host factory. ExerciseComponent resolves
ExerciseAnalytics inside `retainedInstance`. Shared registers ExerciseAnalyticsModule
and its qualified `exercise_analytics` schema. The host retains its toolbar,
Back/close behavior and in-memory session ownership.

## Observed callbacks, operation and schemas

| Owner / callback | Typed fact / version-1 wire schema | Eligibility and meaning |
|---|---|---|
| ExerciseViewModel.onExampleDetailsClick | ExampleDetailsClicked / `exercise_example_details_clicked` | After the existing available exercise/example ID guard, before ShowExampleDetails; observes the click, not downstream display. Loading/unavailable content emits nothing. |
| ExerciseViewModel.onDismiss | DismissRequested / `exercise_dismiss_requested` | Before Back; reports callback invocation without inferring user source or host dismissal. |
| ExerciseViewModel.init / first actual available entity snapshot | LoadHistory / `exercise_task_started`, `exercise_task_completed`, `exercise_task_failed`, `exercise_task_interrupted` | The finite History startup reads the first actual entity snapshot; initial absence ends that attempt without tracking or refreshing. As before, the complete decorator stays in init after actual-entity eligibility, uses that same selected snapshot without a second pre-refresh query, refreshes the 180-day window, reads reconciled trainings and the current entity, and publishes mapped history. History never publishes the entity itself; the throwing refresh remains inside the block. |

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

Loaded exercise-image taps open the encapsulated `ExerciseExampleImage` viewer.
Viewer open/close, image loading and animation are deliberately untracked local
presentation actions: they do not invoke the enclosing row callback or add a
screen identity. Existing row actions remain available outside the loaded image.

## State preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| ExerciseExampleImage | Image availability, fullscreen visibility and transition | Runtime only | Component-local `remember` keyed by image value; Coil painter state | No business draft or accepted result; changing the image or recreating the widget closes the viewer. No StateKeeper, route or ViewModel change |
| ExerciseViewModel / ExerciseState | Actual optional exercise: identity, date, ordered iterations and totals | Rebuild | Plain observeExercise(id) and mapper | Normal data State; null exercise is actual unavailable content, never a Loading wrapper or fake ID. No loader, bootstrap or extra snapshot is added to the database subscription. Available fixed-route entity updates retain independently loaded history; actual absence clears it. No editable data. |
| ExerciseState.history | Nonnull immutable prior records | Rebuild | First actual cached entity snapshot, existing complete LoadHistory operation, getTrainings/observeTrainings, SelectExerciseHistoryUseCase and mapper | Empty by default. The selected date bounds a 180-day window through that day; at most five strictly prior workouts contribute one recorded matching exercise each. Available entity emissions preserve history, including a result that completes before the primary source publishes; the route fixes the entity ID. The first cached entity snapshot selects or skips the existing initial operation without publishing entity snapshots from the history path. History progress belongs to ExerciseLoader.History during the finite snapshot/read/refresh; populated history remains visible during a retry. No State loading mode or comparison cache. Partial measurements remain actual unavailable values. |
| ExerciseState | Optional entity and nonnull history | Rebuild | Actual source values only | Missing primary entity shows the existing unavailable message. Only the finite History request controls its comparison skeleton while history is empty. No primary cache-loading skeleton, Loading/Available/Unavailable lifecycle wrapper, readiness flag or nullable collection. |
| ExerciseScreen | Scroll position | Runtime only | Existing Compose scroll owner | Presentation-only; no business selection or accepted result. |
| ExerciseVolumeHistoryChart | Tooltip / touched bar | Runtime only | Existing shared BarChart owner | No business selection; recreated from chronological history with the selected entry last. |
| DialogConfig.Exercise / host | id, title, callbacks and session identity | Runtime only | Existing slot and content stack, serializer = null | Retained VM supports ordinary recreation; process death closes the dialog session. No StateKeeper supplier is added. |
| Exercise VM / foundation | loaders, directions and subscription | Runtime only | Existing foundation and retainedInstance | Jobs, callbacks and reporting metadata are not serialized. |
| Calling screen (trainings calendar) | Selected date range and Daily/Monthly mode | Preserve | Existing `TrainingsDraft` Component StateKeeper key | Owned by the calendar. This dialog returns no editable selection/result; detail navigation uses the existing example ID. |

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
remain unknown. The sets section reuses IterationCard.Recorded and pairs actual prior
sets by ordinal in a quiet previous-result column. Optional assistance or added
weight changing does not hide the known effective load; no field-presence basis
is inferred. A partial prior set keeps its known repetitions and unknown-load dash.
Comparison data, loading and accessibility stay in the feature-owned trailing slot,
outside the IterationCard entity content. Nullable recorded-load calculation belongs
to IterationState. ExerciseState reports maximum effective load only when every
positive-repetition set has a known load; an incomplete performed set cannot silently
disappear from the maximum. The fixed context width and two-line minimum reserve the
same space for shimmer, prior values and absent matches.

The compact summary after the sets shows only current volume, repetitions and
maximum effective load; set count already belongs to the Sets heading.
Secondary history shows a concise previous value/date and guarded volume percent.
History selection already guarantees the same exercise example. Stored kilogram
volume totals can be compared without reconstructing per-set load bases. The
percentage requires positive previous tonnage, nonnegative current tonnage and a
finite result representable by PercentageFormatState's Int. Zero change displays
0%; zero baseline has no defined percentage. There is no arbitrary 999% ceiling.
The feature checks finite nonnegative tonnage once before rendering the chronological
chart; otherwise explanatory copy occupies its reserved region. The chart component
renders supplied entries, including a single entry, without repeating that business
eligibility check or silently dropping unsupported history. Its empty-input guard
only avoids reading a nonexistent final date. Negative effective set load is an
allowed machine convention, distinct from nonnegative tonnage; signed recorded facts
remain visible without clamping them to fabricated zero. History loading has one
localized indeterminate accessibility node, decorative shared shimmer slots and
the same caption/chart geometry as loaded or empty history. It introduces no
draft, timer, request, retry or business selection.

History refresh failures use the standard `safeLaunch` / ErrorProvider pipeline.
The host shows the general error popup above the open Exercise session; its Retry
runs the next history attempt in the same Job of this VM (each attempt is a new LoadHistory operation)
and Back returns to the session with the cached record. There is no local
retry/failure state or retry event in this feature; re-entry rebuilds the record
and history. Cancellation uses
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
contract. The startup decorator remains after the first actual entity snapshot
within the same initialization path; ExerciseState stores a nonnull immutable
history list, while ExerciseLoader.History supplies
history operation progress. Empty history and unsupported stored tonnage use a
neutral BannerCard in the existing reserved plot region. The admin
section retains its live configuration evidence; layout and chart eligibility
changes add no analytics-facing symbols or admin consumer dependency.
