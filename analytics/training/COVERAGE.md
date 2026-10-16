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
| Completed, saving loader then create/update confirmation and summary; saved-without-details or unconfirmed-result fallback | TrainingCompletedComponent → TrainingCompletedViewModel | `TrainingScreen.Completed` / `training_completed`; typed hook |
| `DialogConfig.ConfirmTrainingCompletion(title, initial, onResult)`, opened only by the recording screen; duration input, Confirm, Cancel | ConfirmTrainingCompletionComponent → ConfirmTrainingCompletionViewModel | Live bottom sheet; default null screenPresentation under the dialog policy; no `TrainingScreen` entry; `hasSameContentAs` compares `initial` |
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
| ConfirmTrainingCompletion duration / confirm / cancel | DurationOpened before the PickDuration direction (the host pushes DurationPicker as a nested step); DurationSelected on the accepted nested result; ConfirmClicked before the Confirm direction (the host delivers the result only for a valid duration, and the UI disables Confirm for Invalid/Empty); DismissRequested from Cancel or system Back, then `finish(null)` |

Seven typed operations decorate complete existing Feature/UseCase calls with
`getOrThrow()` inside the block:

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
  otherwise, and stops uncommitted (no DeleteDraft/LoadTraining/RefreshExamples,
  unconfirmed-result fallback) when the returned ID is null; a nonnull ID is committed to State
  immediately. The written duration is computed at save time from the route
  `startAt`.
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
not that an ID was returned.

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
| Completed | `savedTrainingId` | Preserve | `DRAFT = PersistenceKey("TrainingCompletedDraft", TrainingCompletedState.serializer())`; `restoreState(DRAFT) ?: TrainingCompletedState()` (non-null); default null | Committed immediately after a nonnull save; a restored ID skips Create/Update and resumes DeleteDraft/LoadTraining/RefreshExamples and presentation; a null returned ID stops the chain uncommitted |
| Completed | `title`, `description`, `timeline`, `training`, `muscleLoad` | Rebuild | `@Transient`; localized UiText resources selected from route stage; `observeTraining(id).firstOrNull()` plus timeline/distribution use cases | Copy rebuilds before startup: Edit confirms an update, Add/Draft a completed workout; details rebuild by ID. No localized snapshot is archived |
| Completed | Route inputs `stage`, `exercises`, `startAt` | Rebuild from route | `TrainingRouter.Completed` constructor inputs | Stage seeds rebuilt confirmation copy; all three inputs also serve the save branch, whose duration is computed at write time |
| Completed | SaveTraining loader; Statistics/Exercise dialogs | Runtime only | VM loader and DialogConfig | Not archived |
| Training container | Child stack: Recording / Exercise(seed, result key) / Exercises(Library) / Completed(stage, exercises, startAt) | Preserve (navigation) | `TrainingRouter.serializer()`, key `TrainingComponent`; `RootRouter.Training(stage)` preserved by the root stack | The parent re-registers its result observers in init; route inputs remain ordinary business inputs; no duplicate container draft |
| Training container | `TrainingState.exercises` | Runtime only (never written) | Container state without `update` calls | No DRAFT; the container holds no business draft |
| ConfirmTrainingCompletion dialog | `duration` (DurationFormatState), including the accepted nested-picker value before Confirm | Runtime only (explicit user decision: the existing unsaved-dialog policy) | State not serializable; retainedInstance; host session/step serializers null; no DRAFT | Initial from the parent's elapsed time since startAt, coerced to at least one minute; ordinary recreation keeps the live VM; process death closes both dialog steps |
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

## Verification

Source gates for the current revision were reconciled against actual routes,
render bodies, the three leaf hooks, null containers, delegated Library ownership,
the four retained injections, common foundation binding, the complete
catalog/encoders/callers, current eligibility and the absence of synthetic
init/error-navigation interactions; the preservation gate covered local
suppliers/serializers, restored-empty precedence, accepted parent seeds, transient
data/runtime fields and completed-outcome rebuilding (read-only audit on
2026-10-15; no build was part of it). Checks recorded for the implementing
revisions (2026-10-13):

- Training instrumentation and recreation preservation: final source/diff review
  and `git diff --check` passed; inline invariants confirmed every event consumer
  and matrix entry, seven operation consumers, three leaf hooks/draft suppliers,
  the null container, transient completed presentation, analytics dependency
  boundaries and shared module registration; an independent read-only reviewer
  found no blocking source findings and confirmed the absence of synthetic
  Add/Back events; generated Koin source holds the TrainingAnalytics binding and
  one qualified `training_analytics` provider. Combined
  `:analytics:training:compileKotlinIosSimulatorArm64`,
  `:ui-screen-features:training:compileKotlinIosSimulatorArm64`,
  `:shared:compileKotlinIosSimulatorArm64` and `:androidApp:assembleDebug` passed
  (BUILD SUCCESSFUL; initial integrated run 1m 15s, final rerun 15s). The
  initial sandboxed Gradle wrapper could not write its cache lock; authorized
  execution succeeded. A residual review found no orphan declarations, unused
  imports, missing suppliers or synthetic restoration callbacks. Concurrent
  home/profile/transport working-tree changes were preserved.
- Confirm training completion dialog: source invariants and `git diff --check`
  passed; `:ui-dialog-features:confirm-training-completion:compileAndroidMain`,
  `:analytics:training:compileAndroidMain` and both
  `compileKotlinIosSimulatorArm64` tasks passed together (BUILD SUCCESSFUL,
  5m 12s); the follow-up review rechecked all four event consumers, exhaustive
  encoders, retained injection, generated bindings and the accepted parent state
  path.
- Scoped Home and Training flows review: authorized `:androidApp:assembleDebug`
  and `:shared:assembleSharedDebugXCFramework` (device and simulator frameworks)
  passed after adding the shared `Network.IncompleteResponse` type and removing
  auth response sanitation. Training set/update preserve the existing successful
  server-write outcome when the optional readback cannot hydrate; that review
  also removed redundant example snapshot IDs, and required exercise-example
  snapshots validate completely before writing. These data-boundary changes add
  no screen draft or event/operation payload.

Android Completed saved-result restoration was verified after Home, a confirmed
cached-process kill via `am kill`, and relaunch with a new PID: the saved summary
and values were restored. No duplicate training was observed.

Not verified: StateKeeper save → platform serialization → destroy → fresh-context
restore for Recording and Exercise on Android, and all Training owners on iOS
(including the Essenty codec for LocalDateTime and
nested immutable lists); result-bus ordering between the library pop and the
Recording push; foundation Initial/Restore delivery; Firebase transport; generated
Koin content at runtime; the real `onMove` frequency during a drag; a null
`observeTraining(id).firstOrNull()` right after save; the 300 ms Start dialog
race against an early Add tap; reporting-failure and cancellation injection.
Manual checks not run: Add/Edit/Draft exact and empty exercise lists, pending
iteration formats/order and accepted dialog values; editor replacement-library,
Back, pop and re-entry; pause/resume beyond the subscription grace;
restore during an in-flight write;
SDK delivery. Compilation does not prove these scenarios.
