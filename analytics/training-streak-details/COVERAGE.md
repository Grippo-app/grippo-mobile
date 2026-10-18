# Training streak details coverage

Scope: `:ui-dialog-features:training-streak-details` and its Home entry.

## Surface and analytics gate

Actual entry: HomeViewModel.onOpenTrainingStreak checks the current range, emits
its existing HomeEvent.TrainingStreakClicked and opens DialogConfig.TrainingStreakDetails.
DialogContentComponent constructs TrainingStreakDetailsComponent with that range;
its retained TrainingStreakDetailsViewModel renders TrainingStreakDetailsScreen.
This is one read-only bottom sheet, with no nested flow or independent screen.
The VM keeps default null screenPresentation. No screen enum or dedicated dialog
presentation contract is required or introduced. Surface coverage gate passes source
review of config, factory, Component, VM and rendered content.

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| TrainingStreakDetailsViewModel.onBack | DismissRequested / training_streak_dismiss_requested v1 | Existing callback has no guard; empty parameters, emitted before existing Back direction |
| HomeViewModel.onOpenTrainingStreak | Existing HomeEvent.TrainingStreakClicked | Nonnull range guard; unchanged parent-owned opening intent |
| observeTrainings / provideTrainingStreak | Deliberate omission | Reactive data rebuilding and range-bound metric calculation from performed active days, no explicit request or submission to decorate |

The Component registers a Decompose BackCallback targeting the VM onBack.
However, DialogScreen also installs a Compose BackHandler targeting the host
DialogViewModel.onDismiss(null); toolbar Back and Close are host-owned too.
These paths, swipe and scrim dismissal can bypass the leaf VM and emit no
training_streak_dismiss_requested event. The event therefore observes only the VM
callback request, not every system Back, close, delivered dismissal or cancellation.
Views/close, scrolling and passive cards are omitted. No callbacks, route context,
operation scaffolding, lifecycle jobs or reporting state are added solely for telemetry.
No ranges, dates, training IDs, metric values, raw text or exceptions are encoded.
The internal exhaustive encoder owns one versioned empty schema. The annotated
module registers its qualified training_streak_details_analytics catalog; shared
wires the module and UI depends only on analytics-api. Ordinary reporting Exceptions
cannot prevent Back navigation; fatal Errors propagate. API consumers and retained
VM injection are reconciled against actual source.

## State preservation gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| Component / VM | Immutable range input and its display | Runtime only for live dialog | Caller range, DateRangeFormatState.of, retainedInstance | Ordinary recreation retains VM; reopening uses current caller range |
| VM | streak, insights and timeline | Rebuild | TrainingFeature.observeTrainings combined with currentDateFlow, TrainingStreakUseCase.fromTrainings(trainings, range) with now captured once by the calculator, existing toState mapper | Fresh VM starts with null streak; empty trainings clear it; nonempty data rebuild presentation. Multiple sessions count once per local date; completed weeks/cycles provide evidence; historical ranges are evaluated at their own end |
| Dialog host | Session, callbacks and Back direction | Runtime only | Slot and step serializer = null | Process death discards live dialog; reopening constructs a fresh session |
| Foundation / widgets | Jobs, loaders, scrolling and telemetry provenance | Runtime only | Existing scope and widget ownership | No business draft supplier or synthetic callbacks |

No editable inputs, selections, filters, accepted results, parent drafts or secrets
are owned here. The immutable range is a seed, not an editable filter. Source gate
passes with no StateKeeper snapshot required: session persistence would conflict
with the dialog policy, while presentation is rebuilt from its existing data source.
Initialization, empty data handling, Back and parent opening behavior remain unchanged.
No feature storage, serialization plugin or platform lifecycle bridge is introduced.
The existing WhileActive subscription owns the date flow; it emits on collection
and each local midnight. Reopening or reactivation recalculates with fresh time;
the dialog range itself stays fixed while its final day becomes completed.
Range captions and timeline dates use DateRangeFormatState.short in composition,
so the current locale rebuilds text without changing the runtime date bounds.

## Calculation and presentation boundary

The use case uses performed training days within the fixed caller range, evaluated
at the earlier of its end and now. Repeated sessions on one local date count once.
Positive saved repetitions identify performed activity independently of the
availability of exercise catalog details.
The UI describes inferred recent rhythm rather than a prescribed goal; internal
fit scores are not shown as a probability. Timeline rows retain complete calendar
week/cycle bounds and identify partially observed periods, without treating an
unfinished period as a failure. Empty timelines have no section title.
Detected cycles count only their expected training positions toward completion
and remaining capacity; activity on rest positions does not replace them. Weekly
targets continue to accept any distinct training date in the week.

No event, operation, schema, encoder, screen hook, consent or transport symbol
changes with this calculation update; the live admin synchronization trigger does
not apply. Source, build and device verification evidence is reported in the task
reply rather than kept as historical narratives in this coverage record.
