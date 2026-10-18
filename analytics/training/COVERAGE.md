# Training analytics coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-screen-features:training`, `TrainingRouter`
in `:ui-screen-features:screen-api`, the `:ui-dialog-features:confirm-training-completion`
dialog (it reports through `TrainingAnalytics`) and `:analytics:training`; replace
them when that code changes. Typed dismissal observations use DismissRequested
with `dismiss_requested` wire codes; operation schemas omit visible-screen context
and correlate outcomes by `operation_id`; the app is unreleased and every schema is
version 1.

## Entry paths and navigation

- `RootRouter.Training(stage)` opens `TrainingComponent` with
  `TrainingRouter.Recording(stage)` as the initial stack entry. Callers: Main's
  Add action (`StageState.Add`), the DraftTraining dialog's Continue
  (`StageState.Draft`), the Trainings calendar menu Edit (`StageState.Edit(id)`),
  and the `training_draft` deeplink from the reminder notification scheduled by
  the recording screen.
- `TrainingRouter.Exercise(exercise, resultKey)` is pushed by the recording
  screen after an accepted library selection or an Edit intent;
  `TrainingRouter.Exercises(ExercisesRouter.Library)` hosts the delegated
  exercise library ([exercises](../exercises/COVERAGE.md)) in Default mode from
  the recording screen (key `exercise-library-add`) and SimilarTo mode from the
  exercise editor (key `exercise-library:<exerciseId>`);
  `TrainingRouter.Completed(stage, exercises, startAt)` replaces the whole stack
  after an accepted completion confirmation. `Exercise.Action` is deliberately not
  serializable; result keys serialize by address.
- The stack uses `TrainingRouter.serializer()` with key `TrainingComponent`; all
  routes are serializable. Completed's Back and the container's close return to
  the caller.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Typed presentation / ownership |
|---|---|---|
| Recording, Add/Edit/Draft including the draft deeplink; one screen for all modes | TrainingRecordingComponent → TrainingRecordingViewModel | `TrainingScreen.Recording` / `training_recording`; typed hook |
| Exercise editor, new/edit/replacement-library return | TrainingExerciseComponent → TrainingExerciseViewModel | `TrainingScreen.Exercise` / `training_exercise`; typed hook; original navigation/result owner |
| Exercise editor identity while observed catalog details are unavailable or still belong to the previous selection | Same Exercise owner; no extra surface | Passive Medium reads the existing exercise snapshot's value; no click/chevron or inferred load type. Details eligibility and `ExampleOpened` require observed metadata matching the current example ID |
| Completed, saving loader then create/update confirmation and summary; saved-without-details or unconfirmed-result fallback | TrainingCompletedComponent → TrainingCompletedViewModel | `TrainingScreen.Completed` / `training_completed`; typed hook |
| `DialogConfig.ConfirmTrainingCompletion(title, initial, exercises, onResult)`, opened only by the recording screen; recorded exercise/set summary, duration input, Save workout, Keep training | ConfirmTrainingCompletionComponent → ConfirmTrainingCompletionViewModel | Live bottom sheet; default null screenPresentation under the dialog policy; no `TrainingScreen` entry; `hasSameContentAs` compares `initial` and `exercises`; summary counts only `!isPending` sets and exercises containing them |
| Exercises / Library | ExercisesRootComponent → ExerciseLibraryComponent → ExerciseLibraryViewModel | Delegated `ExercisesScreen.Library` / `exercise_library`; owned by the exercises feature; ExercisesRoot is a container |
| Training stack host (`RootRouter.Training`) | TrainingComponent → TrainingViewModel | Default null; renders only the child stack; forwards navigation; resolves no analytics |

All three `TrainingScreen` entries have exactly one owner. The four retained
consumers of `TrainingAnalytics` (three leaves and the completion dialog) resolve
it inside their `retainedInstance` factories. Foundation remains the sole lifecycle
binding owner and assigns Initial/Restore; no lifecycle listener, reporting state
or route attribution exists.

## Callbacks and operations

| Owner / callbacks | Observation and eligibility |
|---|---|
| Recording onAddExercise / onEditExercise / onDeleteExercise | AddExercise (no guard; the button is always rendered); EditExercise only after the exercise is found; DeleteExercise without a guard, before removal. The empty-start confirmation invokes the library without a synthetic Add click |
| Recording reorder start / end | Start emits nothing and keeps the existing ID/index guards; ReorderExercises emits at accepted end only when the list is nonempty, and the drag-drop list fires its end callback only after a real move, avoiding per-drag volume |
| Recording onSave / onBack | Save emits first, before the pending-sets check; unfinished sets open the confirmation (UnfinishedOpened, then UnfinishedConfirmed in `onResult`); the completion dialog opens only when completed sets exist (CompletionOpened, then CompletionConfirmed in `onResult`, followed by the startAt recompute, pending strip, reminder cancel and ToCompleted), otherwise it returns silently, so Save or UnfinishedConfirmed can occur with no following CompletionOpened. Back (toolbar or system) emits DismissRequested unconditionally; an empty list discards the draft and exits without a dialog, otherwise DiscardOpened and DiscardConfirmed in `onResult` |
| Recording StartTraining (Add, fresh initialization only) | After a 300 ms delay and only while exercises are still null: StartOpened; the empty-start action emits StartConfirmed and opens the library without AddExercise; accepted exercises emit StartConfirmed after a nonempty guard, update state and save the draft |
| Recording library / editor results | LibrarySelected on the accepted library result (key `exercise-library-add`), then ToExercise; ExerciseUpdated on every editor Sync (key `exercise`), that is each accepted iteration add/edit/delete, reorder end, example swap and the editor's Save, each followed by SaveDraft; the editor's Remove calls the business removal path without a fake Delete click |
| Exercise add / edit iteration | AddIteration after the current example exists; EditVolume/EditRepetitions after the example and index guards; IterationOpened when the iteration dialog is constructed and IterationConfirmed on the accepted result |
| Exercise delete / reorder / end / save / back | DeleteIteration without a guard; ReorderIterations per accepted move after the same-ID and index guards (the drag-drop list calls `onMove` per midpoint crossing); ReorderFinished at drag end with no VM guard, then Sync to the parent; Save, then Sync and Back; DismissRequested from the toolbar or system Back |
| Exercise details / replacement library | ExampleOpened unconditionally; ReplaceExample on the detail dialog action, then ToLibrary SimilarTo; ExampleSelected after the different-ID guard (the swap copies name/example and keeps the exercise ID) |
| Completed summary / exercise / back | SummaryOpened after the loaded-training guard (`state.training.id`, Statistics dialog); ExerciseOpened without a guard; DismissRequested from "Done" or system Back, then close to the entry host. Result fallbacks retain Done without a retry or second save. Error-dismissal navigation after a failed save bypasses the user Back event |
| ConfirmTrainingCompletion duration / save / keep training | DurationOpened before the PickDuration direction (the host pushes DurationPicker as a nested step); DurationSelected on the accepted nested result; ConfirmClicked only after the VM resolves `DurationFormatState.Valid`, then the Confirm direction carries that same value. Save workout is disabled for Invalid/Empty, and direct invalid callbacks emit nothing and navigate nowhere; the host forwards the non-null raw duration. DismissRequested from Keep training or system Back, then `finish(null)`. The read-only exercise/set summary adds no interaction or telemetry |

Seven typed operations decorate complete throwing Feature/UseCase calls inside the block:

- DeleteDraft: Add initialization, Edit initialization, the list becoming empty,
  discard, and the Completed cleanup after a successful save.
- LoadDraft: `getDraftTraining().firstOrNull()` on Draft initialization.
- LoadTraining: `observeTraining(id).firstOrNull()` on Edit initialization and in
  the Completed summary; a null local read stays a returned value.
- SaveDraft: `setDraftTraining(draft)` after each accepted mutation; reminder
  scheduling runs outside the block.
- CreateTraining / UpdateTraining: one Completed `init` block under the
  SaveTraining loader skips the write when a saved ID is already present, calls
  `setTrainingUseCase` when the stage has no ID or `updateTrainingUseCase`
  otherwise. The repository succeeds only after a nonnull ID, readback of that
  same training and complete transactional cache hydration. Create/update verify
  the cached active profile required by the cache's foreign key before the remote
  mutation. The UseCase also
  requires its user refresh to confirm an existing profile before returning the
  saved ID; a false profile result fails the UseCase. Mutation/readback
  failures retain their original errors; missing IDs or incomplete/mismatched
  payloads fail with IncompleteResponse. Nested exercises/iterations must have
  matching parent IDs and unique primary IDs; consumed creation dates parse
  before cache insertion. Cancellation propagates. These failures
  reach the existing error pipeline before savedTrainingId or draft cleanup is
  committed. The API chain returns a non-null saved ID, which is committed to
  State immediately after the whole UseCase succeeds. The written duration is
  computed at save time from the route `startAt`. A failure after the server write does not establish
  that the write was rolled back; no automatic retry is introduced.
- RefreshExamples: `getExerciseExamples()` after the Completed save chain.

The recording initialization chain runs only when the restored or initial
exercises are null. Continuous catalogs, validation, total calculations, the
body-weight lookup and notification scheduling are not artificial operations; the
exercise editor has no operations. No coroutine, retry, dispatcher, cancellation
guard or business branch exists for telemetry.

## Catalog and reporting decision

`TrainingAnalyticsModule` (`@Module @ComponentScan`) binds `TrainingAnalyticsImpl`
as `TrainingAnalytics` and registers one qualified `training_analytics` catalog
with five version-1 schemas: `training_interaction` [training_action] and
`training_task_started` [training_operation], `training_task_completed/failed/interrupted`
[training_operation, duration_ms]; `shared/.../Koin.kt` lists it once. The
exhaustive encoder maps all 36 typed events (Recording 16, Exercise 13,
CompletionConfirmation 4, Completed 3) to closed `training_action` codes:

- Recording: `recording_add_exercise`, `recording_edit_exercise`,
  `recording_delete_exercise`, `recording_reorder_exercises`, `recording_save`,
  `recording_dismiss_requested`, `recording_start_opened`,
  `recording_start_confirmed`, `recording_library_selected`,
  `recording_exercise_updated`, `recording_unfinished_opened`,
  `recording_unfinished_confirmed`, `recording_completion_opened`,
  `recording_completion_confirmed`, `recording_discard_opened`,
  `recording_discard_confirmed`.
- Exercise: `exercise_add_iteration`, `exercise_edit_volume`,
  `exercise_edit_repetitions`, `exercise_delete_iteration`,
  `exercise_reorder_iterations`, `exercise_reorder_finished`,
  `exercise_example_opened`, `exercise_replace_example`,
  `exercise_example_selected`, `exercise_save`, `exercise_dismiss_requested`,
  `exercise_iteration_opened`, `exercise_iteration_confirmed`.
- CompletionConfirmation: `completion_confirmation_duration_opened`,
  `completion_confirmation_duration_selected`, `completion_confirmation_confirm`
  (typed event ConfirmClicked), `completion_confirmation_dismiss_requested`.
- Completed: `completed_summary_opened`, `completed_exercise_opened`,
  `completed_dismiss_requested`.
- Operation codes: `delete_draft`, `load_draft`, `load_training`, `save_draft`,
  `create_training`, `update_training`, `refresh_examples`; all seven have VM
  callers, and every event variant has at least one caller.

Operation lifecycle uses the common fresh OperationId/monotonic duration decorator,
preserving caller values, original errors and cancellation (interrupted on
cancellation, failed otherwise; the failed payload drops the error object).
Ordinary reporting exceptions are isolated in `trackEvent`; fatal Errors remain
fatal. The `training_action` code embeds the observed owner without transported
cross-screen reporting context; the four completion-confirmation codes extend the
same schema additively. Admin consumers (the `grippo_training_create_completed`
derived key event on `training_task_completed` with `create_training`) are
reconciled in the root GA4 review; completion proves the operation completed,
including the current repository's required ID and training hydration. Earlier
builds could report completion with a null ID or failed readback; historical
events do not establish the corrected current-build outcome. Wire names,
schemas, decorators, tracking placement, consent and transport are unchanged;
only business-result propagation and its coverage description changed, so no
live admin synchronization trigger is introduced by this fix.

## Omissions and privacy

No raw weights, repetitions, duration or date values, exercise names, entity IDs,
input lengths, credentials, error messages, URLs or bodies enter reporting.
Dialog presentation/close/dismiss/cancel outcomes (StartTraining, Confirmation,
ConfirmTrainingCompletion, Iteration, Statistics, Exercise, ExerciseExample),
scrim/swipe dismissal, internal unaccepted picker edits (the nested DurationPicker
reports its own facts; see [duration-picker](../duration-picker/COVERAGE.md)),
focus/IME, referrer/funnel attribution, per-drag reorder moves on the Recording
screen, the editor Remove result, reminder scheduling/cancel, muscle/example
catalog flows and the error-dismiss Back are deliberate omissions. Consent,
identity and SDK delivery remain application/core-owned.

Accepted risk (explicit user decision): Add remains available before the initial
Edit/Draft source load finishes and during the 300 ms window before the Add-mode
Start dialog. Early Add can establish an empty draft and skip the original source
load. No guard or disabled state was added; this is retained by request, not a
fixed finding or a preservation pass.

The completed-session summary labels mapped iteration counts as Available sets,
including zero. Saved duration, repetitions and volume remain independent recorded
aggregates; the muscle section explicitly scopes analysis to available details.
This presentation change preserves TrainingScreen.Completed, all callbacks and
operations, and the existing savedTrainingId restoration owner; no analytics-facing
symbol, consent or persisted field changes.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| Recording | `stage` | Preserve | `DRAFT = PersistenceKey("TrainingRecordingDraft", TrainingRecordingState.serializer())`; nullable initialState; `saveState` of the latest VM state | The route stage is used only without an archive; Draft loading rewrites the stage to Add or Edit(trainingId), and that resolved value is archived |
| Recording | `exercises` (nullable immutable list) including iteration values with pending/empty formats, IDs, order, example identity, createdAt and totals | Preserve | ImmutableListSerializer; serializable ExerciseState/IterationState | Null means the initial source load is pending, a nonnull empty list is a real empty draft; the initialization chain runs only when restored exercises are null (Add also re-runs DeleteDraft and may reopen the Start dialog); providers return early once nonnull |
| Recording | `startAt` | Preserve | Required field, no default | `DateTimeUtils.now()` only without an archive; Edit/Draft loading sets now minus duration; completion confirmation sets now minus the confirmed duration; the ticking text is rebuilt in the Screen |
| Recording | `examples`, `muscles` | Rebuild | `@Transient`; example observation by current example IDs, muscle observation | Populated by flows without touching the exercises |
| Recording | Room draft training (data layer) | Rebuild from the durable source | `setDraftTraining` after each accepted mutation, `deleteDraftTraining` on empty/discard/completion cleanup, `getDraftTraining` for the Draft stage | The draft deeplink and Main's Continue re-enter through `StageState.Draft` |
| Recording | Reminder notification; StartTraining / Confirmation / ConfirmTrainingCompletion sessions | Runtime only | NotificationManager; live DialogConfig callbacks | Accepted results are applied to State before navigation; sessions are unsaved |
| Recording | Accepted library result; accepted editor Sync/Remove | Preserve at parent | Result keys `exercise-library-add` and `exercise`; the accepted library example seeds the editor route; Sync/Remove update the parent and save the draft | Re-entry seeds the editor from the parent's accepted exercise; a popped editor archive is discarded with its child context |
| Exercise | `exercise` (ExerciseState) | Preserve | `DRAFT = PersistenceKey("TrainingExerciseDraft", TrainingExerciseState.serializer())`; nullable initialState | The route seed is used only without an archive, so the local draft wins; the exercise ID never changes (a swap copies name/example), keeping the SimilarTo result key stable |
| Exercise | `exerciseExample`, `previousIterations`, volume/repetition artifact IDs | Rebuild | `@Transient`; example observation by current ID; validation flow | Recomputed from the restored exercise without replaying callbacks |
| Exercise | Iteration dialog session; body-weight lookup | Runtime only | DialogConfig.Iteration; WeightHistoryFeature | The accepted iteration enters the exercise and reaches the parent through Sync/Remove |
| Completed | `savedTrainingId` | Preserve | `DRAFT = PersistenceKey("TrainingCompletedDraft", TrainingCompletedState.serializer())`; `restoreState(DRAFT) ?: TrainingCompletedState()` (non-null); default null | Committed only after successful required training readback, hydration and UseCase user refresh; a restored ID skips Create/Update and resumes DeleteDraft/LoadTraining/RefreshExamples and presentation. Repository failures keep this checkpoint uncommitted and preserve the durable draft |
| Completed | `title`, `description`, `timeline`, `training`, `muscleLoad` | Rebuild | `@Transient`; localized UiText resources selected from route stage; `observeTraining(id).firstOrNull()` plus timeline/distribution use cases | Copy rebuilds before startup: Edit confirms an update, Add/Draft a completed workout; details rebuild by ID. No localized snapshot is archived |
| Completed | Route inputs `stage`, `exercises`, `startAt` | Rebuild from route | `TrainingRouter.Completed` constructor inputs | Stage seeds rebuilt confirmation copy; all three inputs also serve the save branch, whose duration is computed at write time |
| Completed | SaveTraining loader; Statistics/Exercise dialogs | Runtime only | VM loader and DialogConfig | Not archived |
| Training container | Child stack: Recording / Exercise(seed, result key) / Exercises(Library) / Completed(stage, exercises, startAt) | Preserve (navigation) | `TrainingRouter.serializer()`, key `TrainingComponent`; `RootRouter.Training(stage)` preserved by the root stack | The parent re-registers its result observers in init; route inputs remain ordinary business inputs; no duplicate container draft |
| Training container | `TrainingState.exercises` | Runtime only (never written) | Container state without `update` calls | No DRAFT; the container holds no business draft |
| ConfirmTrainingCompletion dialog | `duration` (DurationFormatState), including the accepted nested-picker value before Confirm | Runtime only (explicit user decision: the existing unsaved-dialog policy) | State not serializable; retainedInstance; host session/step serializers null; no DRAFT | Initial from the parent's elapsed time since startAt, coerced to at least one minute; ordinary recreation keeps the live VM; process death closes both dialog steps |
| ConfirmTrainingCompletion dialog | `exercises` snapshot and its derived recorded exercise/set counts | Runtime only | Recording State → live DialogConfig → retained dialog VM State; counts use `!isPending`, matching completion cleanup | Snapshot is read-only and does not modify the recording; re-entry receives current parent exercises; nested duration edits retain it; process death closes the session while Recording preserves its own exercises |
| ConfirmTrainingCompletion → Recording → Completed | Confirmed duration | Preserve at parent, then route | Recording sets `startAt = now - result` and replaces the stack with `Completed(stage, exercises, startAt)` | Recording's archive is dropped by `replaceAll`; accepted values live in the Completed route |

The exact `@Transient` sets are Recording (examples, muscles), Exercise (example,
previous iterations, two artifact lists) and Completed (title, description, timeline,
training, muscleLoad). No training ViewModel holds a `var` or `lateinit` member; eligibility
derives from State (`exercises == null`, the current example, `savedTrainingId`,
the training ID). No credentials or tokens enter any archived State;
`@JsonIgnoreUnknownKeys` is not used by these States. Components restore inside
`retainedInstance` and register one lazy supplier of current State; no
source/edit flags, previous-source caches, global store, callback synchronization
or route mutation exists. Old archives without the Completed supplier use fresh
state and the existing save behavior. Completed checkpointing is not transactional
server idempotency: process termination, or a checkpoint made before the
successful ID is captured, can still replay an in-flight write; this record
preserves a known completed business outcome and invents no idempotency
mechanism. Android saved state and completed iOS background checkpoints are the
supported shell boundaries; abrupt active iOS termination has no guaranteed
checkpoint.
