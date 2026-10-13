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
| onExternalWeightChange / onAssistWeightChange / onExtraWeightChange / onRepetitionsChange | iteration_picker_input_changed | Existing formatter status changes only; typed input_field and input_status; equal statuses including Empty→Empty omitted |
| onWeightPickerClick | iteration_picker_body_weight_opened | Existing callback before navigation |
| onBodyWeightSelected | iteration_picker_body_weight_confirmed | Accepted callback; typed formatter status only |
| onPreviousIterationClick | iteration_picker_previous_selected | Only if previousIteration exists |
| onIterationClick | iteration_picker_suggestion_selected | Only if the ID resolves in existing suggestions; ID never encoded |
| onRepetitionSuggestionClick | iteration_picker_repetition_selected | Real chip click, only for a listed suggestion; ordinary Int UI callback, no value encoded |
| onSubmit, Save and keyboard Done | iteration_picker_submit | After existing canSubmit guard for all four component kinds; invalid/empty required fields neither emit nor navigate |
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

Source gates reviewed actual routes and parent hooks, accepted/rejected callbacks,
all schema cases and bounded parameters, reporting isolation, retained injection,
DI/catalog registration, parent supplier/serializer and fresh dialog seeds.
Rendering/layout and existing preview fixtures remain unchanged.
No automated test sources, dependencies or infrastructure were introduced.

Independent review was not performed: no permitted independent local reviewer is
available, and subagent delegation was not authorized. Runtime payload delivery,
reporting-failure injection and native restoration remain unverified.

Manual checks: enter valid/invalid/empty text → recreate Activity → inspect retained
input; choose each kind of suggestion and body weight → confirm → archive parent →
destroy lifecycle/InstanceKeeper → fresh context/VM restore → verify exact accepted
set and fresh edit seed; kill with unconfirmed/nested dialog → verify closed dialogs
and previous accepted parent values. Check all four exercise component kinds,
invalid keyboard Done, absent/stale suggestions, Back versus swipe and actual SDK
payloads/status deduplication. Pause/resume must not reset current input.

Scoped inline source invariants and `git diff --check` passed. Generated Koin source
was inspected: it binds IterationPickerAnalyticsImpl to IterationPickerAnalytics
and provides the qualified iteration_picker_analytics schema catalog.

The first combined build waited on a file-cache lock and was interrupted only after
its exact wrapper PID/command and Codex parent were verified. Retrying with a
separate temporary project cache required `--no-watch-fs` (Gradle rejects its use
with filesystem watching enabled). Early combined/Android attempts then failed
outside this scope on missing commonMain.json transformed metadata in toolkit
analytics API and screen-api. No unrelated infrastructure was changed.

Final isolated analytics Android + iOS Simulator compilation passed:
`:analytics:iteration-picker:compileAndroidMain` and
`:analytics:iteration-picker:compileKotlinIosSimulatorArm64` (`BUILD SUCCESSFUL`,
21s). After dependencies were rebuilt, the final integrated check passed:
`./gradlew --no-configuration-cache --no-watch-fs --project-cache-dir /tmp/grippo-iteration-project-cache :ui-dialog-features:iteration-picker:compileAndroidMain :ui-dialog-features:iteration-picker:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64`
(`BUILD SUCCESSFUL`, 5m 57s). This verifies final UI compilation for Android and
Simulator and shared composition/DI compilation. Earlier metadata failures are
historical, not outstanding build blockers. APK assembly, framework linking,
installation and native runtime were not run.

## Follow-up tail review

Re-reviewed actual source/diff, public facts and exhaustive payload mapping, all
Contract/UI/preview callers, submit/status guards, schema/DI registration and
local/global coverage references. No production-code defect or stale caller was
found. The inventory now includes the downstream TrainingRecording owner and its
existing Sync → updateExercise → saved exercises/business draft chain, previously
missing from this scoped record. Independent review and native runtime remain
unperformed under the limits above. Scoped source checks and git diff --check
passed again; no code changed and no redundant build was run. The prior successful
compilation results apply to the unchanged implementation.
