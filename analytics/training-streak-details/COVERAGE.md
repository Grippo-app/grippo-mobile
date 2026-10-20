# Training streak details coverage

Scope: `:ui-dialog-features:training-streak-details` and its Home entry.

## Surface and analytics gate

Actual entry: the Home Dashboard overview TrainingStreakCard uses scalableClick
to invoke HomeViewModel.onOpenTrainingStreak, which checks the current range, emits
its existing HomeEvent.TrainingStreakClicked and opens DialogConfig.TrainingStreakDetails.
DialogContentComponent constructs TrainingStreakDetailsComponent with that range;
its retained TrainingStreakDetailsViewModel renders TrainingStreakDetailsScreen.
This is one read-only bottom sheet, with no nested flow or independent screen.
The VM keeps default null screenPresentation. No screen enum or dedicated dialog
presentation contract is required or introduced. Surface coverage gate passes source
review of config, factory, Component, VM and rendered content.

| Route / surface | Component | ViewModel | Presentation | Callbacks / operations | Omissions |
|---|---|---|---|---|---|
| HomeRouter.Home / Dashboard entry | HomeComponent | HomeViewModel | Existing HomeScreen.Home hook | onOpenTrainingStreak observes TrainingStreakClicked after the range guard | Other Home behavior belongs to analytics/home/COVERAGE.md |
| DialogConfig.TrainingStreakDetails / read-only sheet | TrainingStreakDetailsComponent | TrainingStreakDetailsViewModel | Default null: dialog, outside independent-screen coverage | onBack observes DismissRequested; reactive calculations have no request operation | Host dismissal, passive reading and scrolling are not tracked |

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
| VM | streak, recent evidence and calculation explanation | Rebuild | TrainingFeature.observeTrainings combined with currentDateFlow, TrainingStreakUseCase.fromTrainings(trainings, range) with now captured once by the calculator, existing toState mapper | Fresh VM starts with null streak and displays the shared loading component. Every emission, including an empty list, is calculated. Zero performed active days display empty guidance; positive active days display the summary and available evidence. Multiple sessions count once per local date; historical ranges are evaluated at their own end |
| Dialog host | Session, callbacks and Back direction | Runtime only | Slot and step serializer = null | Process death discards live dialog; reopening constructs a fresh session |
| Foundation / widgets | Jobs, loaders, scrolling and telemetry provenance | Runtime only | Existing scope and widget ownership | No business draft supplier or synthetic callbacks |

No editable inputs, selections, filters, accepted results, parent drafts or secrets
are owned here. The immutable range is a seed, not an editable filter. Source gate
passes with no StateKeeper snapshot required: session persistence would conflict
with the dialog policy, while presentation is rebuilt from its existing data source.
Back and parent opening behavior use their existing callbacks and routes.
No feature storage, serialization plugin or platform lifecycle bridge is introduced.
The existing WhileActive subscription owns the date flow; it emits on collection
and each local midnight. Reopening or reactivation recalculates with fresh time;
the dialog range itself stays fixed while its final day becomes completed.
Range captions and timeline dates use DateRangeFormatState.short in composition,
so the current locale rebuilds text without changing the runtime date bounds.

## Availability and content

The fixed range caption is visible above one anchored scroll owner. A null streak
means the first source observation has not completed; the shared Loader supplies
localized loading text and indeterminate progress semantics. A successful source
observation always produces a nonnull calculation. An empty list and training rows
whose repetitions are all zero both produce zero active days and the same compact
empty guidance. No zero achievement summary, timeline, method block or unsupported
navigation action is displayed for that state. A failed observation stays in the
existing Flow.safeLaunch error pipeline and is not presented as an empty result.

With active history, the sheet presents one summary, recent evidence when its
timeline is nonempty, and one short calculation explanation. The components own
their headings; the screen adds no duplicate insight or timeline titles. Active
history with a zero current run remains populated so that earlier activity is not
hidden. A missing timeline omits the entire evidence section. Dialog background,
section spacing, text hierarchy and bottom/navigation insets use their existing
design-system owners.

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
changes with this presentation and availability behavior; the live admin
synchronization trigger does not apply. Source, build and device verification evidence is reported in the task
reply rather than kept as historical narratives in this coverage record.
