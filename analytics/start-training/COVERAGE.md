## Current contract follow-up (2026-10-14)

The [current analytics review](../COVERAGE.md#current-analytics-contract-review-2026-10-14)
records this revision and its verification limits. Typed dialog observation names
use DismissRequested across screens/dialogs, with dismiss_requested wire names/codes.
The app has not been released; all analytics schemas use initial version 1.
This feature's operation schemas use version 1 and omit visible-screen context;
feature operation codes and operation_id own outcome correlation.

# Start training dialog coverage

Scope: `:ui-dialog-features:start-training` and its accepted-result boundary in Recording.

## Surface and observation owners

| Route / rendered surface | Component → VM | Presentation policy |
|---|---|---|
| `DialogConfig.StartTraining`, opened by TrainingRecording Add initialization while exercises are null; shared dialog content factory renders StartTrainingScreen | StartTrainingComponent → retained StartTrainingViewModel | Live bottom sheet, no dedicated dialog presentation contract; default null screenPresentation, no independent screen identity |

The existing TrainingRecording parent owns StartOpened/StartConfirmed and its
Recording screen hook. Shared DialogComponent/Content host sessions and steps use
null serializers. StartTraining owns no additional host, screen or lifecycle wiring.

| Callback / operation | Observation | Eligibility and payload |
|---|---|---|
| onSelect(key) | start_training_option_confirmed v1 | Only after key resolves in current options; option enum empty/preset/recent. Unknown keys emit nothing and navigate nowhere |
| onBack() including registered hardware Back | start_training_dismiss_requested v1 | No payload; existing Back navigation is unchanged |
| Startup options observation (Processing.Infinity), before the first cache emission | BuildOptions / start_training_task_started/completed/failed/interrupted v1 | Whole preset use case run once per dialog opening; operation code and terminal duration only. Preset option mapping runs once after the operation; cache observation, recent mapping and state updates run per emission outside it |

`StartTrainingAnalytics` is injected through the retained VM factory; its internal
implementation and qualified complete schema catalog are registered through the
single generated StartTrainingAnalyticsModule in shared. Event reporting Exceptions
are isolated; operation reporting uses the existing toolkit decorator, preserving
original return/error/cancellation and fresh operation IDs with monotonic timing.
Fatal Errors remain fatal. No IDs, raw keys, exercise values, dates, measurements,
exception text or request/response bodies are encoded.

Deliberate omissions: category tabs, recent-workout date selection and data
population do not emit confirmation. Only the pinned action confirms the current
option key. The read-only plan shows planned sets and repetitions beside each
exercise in the current option. PresetIteration.toState already supplies empty weight
fields for generated plans; recent plans clear weights through the owning VM's
prepareRecentExercise/prepareRecentIteration methods. Both
retain planned repetitions and enter the recording editor as pending sets.
The preview never displays stale exercise totals or completion. Outside/swipe host
dismissal is not synthesized as Back. Dedicated dialog view/close is not defined.
The range refresh calls the throwing `getTrainings` in its own startup safeLaunch
under the Options loader, in parallel with the Room observation that builds options
from the current cache and rebuilds them when the refresh lands. Refresh,
observation and preset failures use the shared error pipeline, and the dialog host
dismisses the sheet on a pipeline error; a refresh failure emits no BuildOptions
start, failure or duration. BuildOptions completed means the preset was
generated, not that a workout started. The Options loader indicates history refresh;
the screen shows the shared Loader only while no preset or recent option exists
yet, and Custom remains an independent valid Empty choice throughout.
No business context or callback is added solely for analytics.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| StartTrainingViewModel | Options including recent identities, required LocalDateTime dates and exercises; generated preset with pending sets | Rebuild on fresh dialog opening | Continuous TrainingFeature cache observation (Processing.Infinity) plus one GeneratePresetTrainingUseCase run per dialog opening; TrainingFeature range refresh in parallel under the Options loader | Cached options render immediately and update when the refresh lands; the preset option and its generated ids are built once and reused across emissions; Empty option remains actionable throughout. No StateKeeper supplier |
| StartTrainingScreen / StartTrainingPage / StartTrainingRecentPicker | selectedChoice, selectedRecentKey; vertical and horizontal list presentation | Runtime only | Screen remembers the category, initially Empty, and nullable recent key; Page pins its introduction and recent picker above the exercise-only vertical list; the action overlays the list through the shared bottom gradient | Late options preserve the current choice. Unaccepted live choice and scroll presentation are discarded with the in-memory dialog session after process death |
| Dialog hosts / Component | Session, callbacks, Back registration, VM jobs, errors, Options loader | Runtime only | Existing retainedInstance; host serializers null; safeLaunch loader lifecycle around the refresh | Ordinary retained recreation differs from process death; no session archive or callback replay. Shared Loader renders only while no preset or recent option exists yet; the pinned Empty action stays available |
| TrainingRecording parent | Accepted preset/recent exercises, cloned exercise/iteration IDs and fresh exercise timestamp | Preserve at existing parent | TrainingRecordingComponent DRAFT / TrainingRecordingDraft; existing saveDraftTraining data source | StartTraining acceptance assigns one current timestamp to all cloned exercises so validation/history uses the new session's cutoff. The Recent chooser retains its historical date. onUseExercises updates parent's exercises and saves draft; no duplicate dialog owner |
| TrainingRecording parent | Accepted StartEmpty marker: exercises=[] | Preserve | TrainingRecordingComponent DRAFT / TrainingRecordingDraft | openExerciseLibrary changes null to an accepted empty list before ToLibrary. Back/restoration does not reopen StartTraining; no durable exercise draft is saved until an exercise is accepted |
| Analytics/foundation | Reporting provenance and operation timing/IDs | Runtime only | Existing toolkit | Never serialized; no synthetic restoration events |

No secret input exists in this dialog. Its local selections/session are explicitly
unsaved by the state-preservation canon. Accepted data already has its screen
owner. No new serializer, PersistenceKey, restore flag or shadow snapshot is needed.
