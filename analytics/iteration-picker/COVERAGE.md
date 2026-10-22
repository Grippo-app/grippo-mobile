# Iteration picker analytics and preservation

Scope: `ui-dialog-features/iteration-picker`, 2026-10-13.

## Surfaces and observation owners

| Route / surface | Component → VM | Presentation policy |
|---|---|---|
| DialogConfig.Iteration, add/edit set from Training exercise | IterationPickerComponent → IterationPickerViewModel | Runtime overlay; default null screenPresentation, no dedicated dialog presentation contract |
| Nested body weight picker | WeightPickerComponent → WeightPickerViewModel | Existing runtime overlay; iteration VM observes its own open and accepted result |
| TrainingRouter.Exercise | TrainingExerciseComponent → TrainingExerciseViewModel | Existing TrainingScreen.Exercise hook, retained injection and foundation binding |
| TrainingRouter.Recording, parent of the exercise editor | TrainingRecordingComponent → TrainingRecordingViewModel | Existing TrainingScreen.Recording hook and saved parent draft |
| Shared dialog slot/content stack | DialogComponent / DialogContentComponent | Containers, no added screen hook |

The sole Iteration config caller is TrainingExerciseViewModel.showIterationDialog.
Add, edit and artifact-correction paths use that owner. Existing parent events
IterationOpened/IterationConfirmed remain at the owning parent. The picker service
is resolved in the existing retained VM factory; shared registers its annotated
module and uniquely qualified complete schema catalog. No async business operation
exists here, so no operation API/model/decorator is added.

| Callback | Wire schema, version 1 | Eligibility and observation |
|---|---|---|
| onExternalWeightChange / onAssistWeightChange / onExtraWeightChange / onRepetitionsChange | iteration_picker_input_changed | Formatter status changes only; typed input_field and input_status; equal statuses including Empty→Empty omitted. Explicit assistance zero is Valid, distinct from Empty; negative fractions and nonfinite assistance are Invalid. Other input zero conventions are unchanged |
| onWeightPickerClick | iteration_picker_body_weight_opened | Existing callback before navigation |
| onBodyWeightSelected | iteration_picker_body_weight_confirmed | Accepted callback; typed formatter status only |
| onPreviousIterationClick | iteration_picker_previous_selected | Only if previousIteration exists |
| onIterationClick | iteration_picker_suggestion_selected | Only if the ID resolves in existing suggestions; ID never encoded |
| onRepetitionSuggestionClick | iteration_picker_repetition_selected | Real chip click, only for a listed suggestion; ordinary Int UI callback, no value encoded |
| onSubmit, Save and keyboard Done | iteration_picker_submit | After canSubmit; body modes require a Valid recorded multiplier; required assistance needs Valid (including zero), optional assistance accepts Empty or Valid. Invalid assistance, missing required assistance or an invalid/missing body multiplier neither emit nor navigate; Save uses the same eligibility. Accepted results clear load components outside the displayed family and preserve the displayed recorded multiplier |
| onBack, including Component system Back | iteration_picker_dismiss_requested | Observed callback before existing navigation |

Suggestion application emits its distinct selection event, without synthetic text
status changes. Programmatic initialization does not emit input events. The chip
callback applies the same RepetitionsFormatState.of(Int) result as the former String
callback for these valid integer suggestions. No raw input/display/length, weight,
repetition count, set number, entity ID, exception or reporting provenance is sent.
The stateless implementation isolates ordinary reporting Exceptions; fatal Errors
remain fatal. Reporting does not change validation, result delivery or navigation.

Dedicated dialog views/closes, host swipe/outside/programmatic dismissal, focus,
clear and keyboard-action attribution remain explicit omissions. Submit does not
claim host result delivery; the host delivers pending callbacks after dismissal.
No reporting context is added to routes, configs, State or business Direction types.

## Preservation inventory

| Owner | Field/group | Policy | Source / restore and check |
|---|---|---|---|
| IterationPickerViewModel | value: external/assist/extra/body weight, repetitions, multiplier and set identity | Runtime session / retained | Existing retainedInstance; unfinished or invalid/empty input remains in VM while session lives; no process-death archive |
| IterationPickerViewModel | example, number, focus, iterationSuggestions, previousIteration | Runtime caller inputs | Fresh session receives current accepted parent data and current suggestions |
| IterationPickerViewModel | repetitionSuggestions | Rebuild | Existing init derives from suggestions plus 6/8/10/12 |
| Screen | FocusRequester, repetitionsFocused, scrolling | Runtime UI presentation | Existing Compose ownership; no business snapshot |
| TrainingExerciseViewModel | exercise.iterations including accepted additions/replacements | Preserve at parent | TrainingExerciseComponent DRAFT: TrainingExerciseDraft / TrainingExerciseState.serializer(); lazy state supplier; restore inside retained factory before fresh VM construction |
| TrainingExerciseViewModel | exerciseExample, previousIterations, validation artifacts | Rebuild | Existing transient fields and current source subscriptions |
| TrainingRecordingViewModel | exercises with accepted set values, identities and order | Preserve at parent | TrainingExerciseDirection.Update → Exercise.Action.Sync → TrainingRecordingViewModel.updateExercise; TrainingRecordingComponent DRAFT: TrainingRecordingDraft / TrainingRecordingState.serializer(), ImmutableListSerializer; existing saveDraftTraining also persists the business draft |
| Shared dialog host | sessions, nested body-weight step, pending result callback | Runtime only | Both host slot and content stack use serializer = null |
| Analytics / loaders / callbacks | telemetry and execution state | Runtime only | No new state key/supplier, secret or lifecycle observer |

Accepted assistance preserves explicit Valid(0) through domain/state mappers and
the existing IterationState serializers; null remains Empty. The dialog adds no
archive field or restoration flag. Zero now contributes the existing Valid input
fact, and missing required assistance no longer contributes an accepted Submit;
event codes, status meanings, schemas and tracking-call arguments are unchanged.
Only an explicitly accepted edit clears hidden load components incompatible with
the displayed example mode; reopening/cancelling does not rewrite a historical
set. The recorded multiplier shown in that edit is retained, never replaced by a
new catalog coefficient at submit.

Parent addIteration/replaceIteration update the exercise in State; the existing
supplier reads that latest state. Fresh edit dialog seeds from current iterations;
a fresh add uses the parent's ordinary current defaults. Accepted body weight is
still unfinished iteration input until iteration submit reaches the screen parent.
Exercise updates also propagate through the existing ResultManager to Recording,
which updates its serializable exercises and saves its existing business draft.
Its local archive is restored before fresh child construction; reopening an editor
uses the current accepted exercise. Neither owner persists unfinished dialog input.
After process death the dialogs close and only accepted parent values restore.
Supported Activity recreation retains the existing VM while the retained host
session survives. No new persistence is necessary under the canonical dialog
policy, and no callback is replayed by restoration. iOS restoration requires a
completed valid background checkpoint; abrupt active termination has no guarantee.

## Verification

Source checks cover actual routes and parent hooks, accepted/rejected callbacks,
status deduplication, typed parameters, retained injection and existing serializer
owners. Explicit zero assistance, missing required assistance and invalid negative
fractions have named AppPreview fixtures alongside the existing component modes.
Nonfinite multipliers are represented as Invalid before rounding.

Manual runtime checks distinguish rendered fixture eligibility from real input,
accepted results and restoration. Check zero versus cleared required input,
negative/nonfinite values, Save and keyboard Done, valid/stale suggestions, Back,
and accepted values after fresh edit entry. Parent restoration must use a fresh
Component/InstanceKeeper and its existing archive; dialog sessions remain unsaved.
Native screenshots and current build/review outcomes belong in the task reply;
static no-op fixtures do not prove result delivery, saving or restoration.

No automated test sources, dependencies, additional telemetry or persistence
infrastructure are introduced by this validation change.
