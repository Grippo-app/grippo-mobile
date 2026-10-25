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
and correlate outcomes by `operation_id`; the typed catalog is not yet in a store build and every schema is version 1.

## Entry paths and navigation

- `RootRouter.Training(stage)` opens `TrainingComponent` with
  `TrainingRouter.Recording(stage)` as the initial stack entry. Callers: Main's
  Add action (`StageState.Add`), the DraftTraining dialog's Continue
  (`StageState.Draft`), the Trainings calendar menu Edit (`StageState.Edit(id)`),
  and the `training_draft` deeplink from the reminder notification scheduled by
  the recording screen.
- `TrainingRouter.Exercise(exercise, resultKey)` is pushed by the recording
  screen after an accepted library selection or an Edit intent.
  `TrainingComponent.toExercises` requests `RootRouter.Exercises(Library)` for
  the exercise library ([exercises](../exercises/COVERAGE.md)) in Default mode
  from the recording screen (key `exercise-library-add`) and SimilarTo mode from
  the exercise editor (key `exercise-library:<exerciseId>`). The root owns the
  picker and its Back; Training and its result observers remain beneath it.
  `TrainingRouter.Completed(stage, exercises, startAt)` replaces the whole stack
  after an accepted completion confirmation. `Exercise.Action` is deliberately not
  serializable; result keys serialize by address.
- The stack uses `TrainingRouter.serializer()` with key `TrainingComponent`; all
  routes are serializable. Completed's Back and the container's close return to
  the caller. Completed's Goal direction (the goal details dialog's add-goal
  action; the Growth goal card itself has no update action) is threaded
  `TrainingCompletedDirection.Goal` → `TrainingCompletedComponent.toGoal`
  (= `TrainingViewModel::toGoal`, a `TrainingContract` relay like its siblings) →
  `TrainingDirection.ToGoal` → `TrainingComponent.toGoal` (= `RootViewModel::toGoal`)
  → `RootDirection.Goal` → `navigation.push(RootRouter.Profile(ProfileRouter.Goal))`
  in `RootComponent.eventListener`. The training stack stays beneath the pushed
  Goal screen; its Back at the Profile root calls `RootViewModel.onBack`, which pops
  back to Completed.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Typed presentation / ownership |
|---|---|---|
| Recording, Add/Edit/Draft including the draft deeplink; one screen for all modes | TrainingRecordingComponent → TrainingRecordingViewModel | `TrainingScreen.Recording` / `training_recording`; typed hook |
| Exercise editor, new/edit/replacement-library return | TrainingExerciseComponent → TrainingExerciseViewModel | `TrainingScreen.Exercise` / `training_exercise`; typed hook; original navigation/result owner |
| Exercise editor identity while observed catalog details are unavailable or still belong to the previous selection | Same Exercise owner; no extra surface | Passive Medium reads the existing exercise snapshot's value; no click/chevron or inferred load type. Details eligibility and `ExampleOpened` require observed metadata matching the current example ID |
| Completed: saving skeletons (`TrainingCompletedSkeleton` under the SaveTraining loader: `TrainingRecapHeroSkeleton`, comparison, weekly volume, streak and session muscle skeletons, no actions skeleton, no records skeleton because achievements are unknown until the save completes, Done in Loading state and Share in the overlay state described below), then the recap hero (`TrainingRecapHero` with `TrainingRecapHeroStyle.Resolved(verdict, baseline, description)`: the period verdict and its line from `PerformanceVerdictState.summary(metrics, baseline)` over `performance.metrics` and `performance.baseline` (the baseline those metrics were computed against: `training_recap_compared` under Average, `training_recap_compared_previous` under Previous, `training_recap_first_description` for a first workout), joined to the Home-period label through `joined_pair` in the Screen, plus workout facts; while `performance` is null the hero renders `TrainingRecapHeroStyle.Pending`, keeping the facts under decorative verdict bones; the hero has no actions row; the hero records into the Screen-level `rememberGraphicsLayer` (`heroLayer`, created in `TrainingCompletedScreen`) the hero as drawn, its four rounded corners over an opaque `background.screen` base (the color the host already paints beneath it) so the translucent warm surface stays readable in the shared PNG, and the layer is what the screen draws; the captured image contains no controls), the records card (`TrainingRecordsCardSkeleton` with a loading semantics node while the `LoadRecords` loader runs, `TrainingRecordsCard` with one row and one tap per exercise example, listing that example's record kinds, when `records` is non-empty, no section otherwise), performance growth bars for Volume, Repetitions, Intensity and Density (the computed Duration metric is not rendered on Completed) with the Average/Previous baseline segment (segment selection, status chips and values all read `performance`, so after a tap the segment keeps the computed baseline until the recomputed metrics arrive and then moves with them) or their skeleton panel (a loading node while `performance` is null), the weekly volume card (its own window: the current ISO week plus `WeeklyVolumeUseCase.TYPICAL_WEEKS` complete weeks before it, independent of the Home period) or its skeleton with the same loading node while `weeklyVolume` is null, the streak skeleton (Growth geometry, silent because it resolves with the comparison skeleton that announces loading) while `performance` is null, then the streak (with the next-training line: the next day from `nextTrainingDate` for a daily or cyclic rhythm, the training days still due this week for a weekly rhythm, no line when no rhythm qualified) and goal Growth cards (a brand-new workout compares against the period before it; an Edit shows the plain headline/score with `previous = null`), and the session muscle load card (no callback on this screen); a one-shot `KonfettiParade` over the content, shown only while `recordCelebration` is `Playing` and the SaveTraining loader has cleared, so a celebration armed during the save (records or the verdict resolving while the loader still covers DeleteDraft and LoadTraining) waits for the content and never plays over the saving skeleton (armed once per screen instance by a period-best verdict or a non-empty records load; the parade reports `onShown` as it starts, which sets `Done`, and then finishes from its own local state; `Done` never re-arms); saved-without-details or unconfirmed-result fallback (Done only, no Share); the bottom overlay stacks the primary Done button and, while saving or once `training` exists, a full-width Secondary Share button (`share` label, Share start icon) outside the captured surface, whose state is Disabled while the SaveTraining loader runs (no hero is drawn then), Loading (not clickable) while `TrainingCompletedLoader.ShareRecap` is in `loaders`, Enabled once the hero style is `TrainingRecapHeroStyle.Resolved` and Disabled otherwise, so Share stays unavailable until the verdict resolves (the image never carries the Pending bones) and while a share is in flight; a tap first scrolls the content list back to the hero (its first item) and settles the hero backdrop (`TrainingRecapHeroState.settle()` ends a running entrance, drops a running verdict crossfade and returns once that settled hero has been drawn), then hands the VM the capture `{ heroLayer.toImageBitmap() }`, which runs inside the ShareRecap operation; any cancellation of that Screen coroutine before `onShareClick` (a drag or fling during the scroll, leaving the screen through Done or Back, a recreation) drops the tap before it reaches the VM, and a second tap during the scroll replaces the first. Completed has no Statistics entry point | TrainingCompletedComponent → TrainingCompletedViewModel | `TrainingScreen.Completed` / `training_completed`; typed hook |
| `DialogConfig.ConfirmTrainingCompletion(title, initial, exercises, onResult)`, opened only by the recording screen; recorded exercise/set summary, duration input, Save workout, Keep training | ConfirmTrainingCompletionComponent → ConfirmTrainingCompletionViewModel | Live bottom sheet; default null screenPresentation under the dialog policy; no `TrainingScreen` entry; `hasSameContentAs` compares `initial` and `exercises`; summary counts only `!isPending` sets and exercises containing them |
| External `RootRouter.Exercises(Library)` destination | ExercisesRootComponent → ExerciseLibraryComponent → ExerciseLibraryViewModel | `ExercisesScreen.Library` / `exercise_library`; owned by the exercises feature; root and ExercisesRoot are containers |
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
| Completed share / record / baseline / metric / streak / goal / add goal / confetti / back | the Screen calls `onShareClick(capture)` only after its content list has scrolled back to the hero and the settled hero has been drawn (any cancellation before that, such as a drag or fling during the scroll, leaving the screen or a recreation, drops the tap, which then emits nothing; a second tap during the scroll replaces the first, so one event); `onShareClick(capture)` returns without an event while the `ShareRecap` loader is active (the overlay's Share button is Loading and not clickable then, and it stays Disabled during the save and until `performance` is computed, so every accepted tap captures the drawn, resolved hero); otherwise ShareClicked, then `safeLaunch(loader = ShareRecap)` runs the ShareRecap operation around the capture and the share; RecordOpened unconditionally in `onRecordClick(exampleId)` (the card is rendered only with loaded records), opening ExerciseExampleDetails (`exercise_details_btn`) for the tapped example; BaselineSelected only when the selected baseline differs from `state.baseline` (a repeated tap on the active segment emits nothing and changes nothing), then the selection is committed to State and the period chain recomputes the comparison, whose metrics and computed baseline replace `performance` in one update; MetricOpened (from the four rendered rows; Completed opens no Duration trend) after the computed-comparison guard (`performance.period.value`), opening PerformanceTrendDetails on the comparison period (title `value_performance_trend(rangeLabel, metric)` when that period is the Home period, otherwise `performance_trend(metric)` with the dates in the dialog's range header); StreakOpened and GoalOpened after the resolved-period guard (`state.range.value`), opening TrainingStreakDetails (`metric_overview_streak_title`) and TrainingGoalDetails (`goal_details_title`) for the Home period; AddGoalClicked only from the goal dialog's `onAddGoal`, then the Goal direction (the Growth goal card has no add/update action); `onRecordConfettiShown` only moves the one-shot `recordCelebration` to `Done` and emits nothing (decorative presentation, not a user action); DismissRequested from "Done" or system Back, then close to the entry host; while the SaveTraining loader runs `onBack` returns without an event (Done is Loading and system Back is ignored, so a user Back never cuts the save off mid-write; process death or a root stack replacement still can). Done stays rendered in Loading state during the save; result fallbacks retain Done without a retry or second save. Error-dismissal navigation after a failed save bypasses the user Back event. A record, metric bar, streak or goal card is only rendered once its data exists, so a rejected guard needs no disabled state; the session muscle card has no callback here. Completed opens no Statistics dialog: by the user's decision the hero's Statistics button was removed together with `onSummaryClick` and SummaryOpened; the dialog is opened only from Trainings |
| ConfirmTrainingCompletion duration / save / keep training | DurationOpened before the PickDuration direction (the host pushes DurationPicker as a nested step); DurationSelected on the accepted nested result; ConfirmClicked only after the VM resolves `DurationFormatState.Valid`, then the Confirm direction carries that same value. Save workout is disabled for Invalid/Empty, and direct invalid callbacks emit nothing and navigate nowhere; the host forwards the non-null raw duration. DismissRequested from Keep training or system Back, then `finish(null)`. The read-only exercise/set summary adds no interaction or telemetry |

Ten typed operations decorate complete throwing Feature/UseCase/toolkit calls inside the block
(ShareRecap also the Screen-supplied capture that feeds its toolkit call):

- DeleteDraft: Add initialization, Edit initialization, the list becoming empty,
  discard, and the Completed cleanup after a successful save.
- LoadDraft: `getDraftTraining().firstOrNull()` on Draft initialization.
- LoadTraining: `observeTraining(id).firstOrNull()` on Edit initialization and in
  the Completed startup chain; a null local read stays a returned value. Completed
  publishes this initial result inside the SaveTraining chain, runs RefreshExamples
  on its own once the saved ID exists, and separately observes
  its saved ID continuously, so later catalog
  translations rebuild nested names without emitting another LoadTraining operation.
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
- RefreshExamples: `getExerciseExamples()` once per Completed screen instance after the saved ID
  exists, in its own `take(1)` chain under `Processing.Infinity` outside the SaveTraining
  loader, so a catalog failure reaches the standard error pipeline without closing the
  screen or holding the saving skeleton. It starts with the saved-ID commit,
  alongside DeleteDraft and LoadTraining, so it can also run in a session whose
  DeleteDraft or LoadTraining later fails (that failure still closes the flow) and
  it ends `interrupted` when the screen closes first; the catalog's usage counts
  and last-used dates then keep their pre-save values until Home next refreshes
  the examples, a known trade-off of running the refresh outside the save.
- RefreshTrainings: one `getTrainings(start, end)` per saved ID and resolved
  Home and comparison periods on Completed, through a nested `safeLaunch` joined
  by `mapLatest` so a handled request failure leaves the period observation
  alive; no loader. The request covers the union of the Home period, the
  comparison period (the same window for a workout saved today) and the
  weekly-volume window (minimum `from`, maximum `to`, with
  `window = WeeklyVolumeUseCase.observationRange(now)`), so everything the screen
  reads (the hero verdict, the comparison, the Growth cards and the "This week"
  card) is refreshed by a single operation, including an edited older workout's
  own period. Changed rolling
  period bounds (a day rollover under a rolling preset such as the Last30Days
  default re-resolves `range`) issue a new union request; the weekly window
  moving alone (a calendar preset within the same week, month or year) issues
  none. As on Home, the chain keeps `Processing.WhileActive`: only a pause that
  outlasts the one-second disconnect grace drops the subscription, so a request
  still in flight then ends as interrupted and the resume repeats
  RefreshTrainings for the same ID and period; a pause shorter than the grace
  leaves the request running.
  The period flows (`observeTrainings` combined with `observeExerciseExamples`
  and `observeGoal`) start only after `savedTrainingId` exists and
  `LocalSettingsFeature.observeRange()` has emitted (the settings repository applies
  the Last30Days default when nothing is stored), so the just-saved workout is already
  in the cache; they publish performance, streak and goal progress (each
  Growth card with its before-this-workout counterpart) without operations, like
  Home. The performance chain combines three inputs into a `PeriodBaseline` carrier:
  the saved training from its own observation (`savedTrainings.filterNotNull()`),
  its comparison period (`observeComparisonPeriods(saved)`) and the distinct
  `state.baseline` selection (a private `observeBaseline()` flow), so a baseline
  toggle re-observes the same cached period and recomputes
  `PerformanceTrendUseCase.fromTraining(saved, period, baseline)` without a new
  RefreshTrainings request; a new emission of the saved training recomputes too.
  The emission carries the baseline and period it was computed with, and
  `provideComparison` publishes the metrics together with them as one
  `PerformanceComparisonState` in a single update; the archived `baseline` stays
  the user's selection. The streak has its own chain on the Home period
  (`observePeriodStreak` → `PeriodStreak` → `provideStreak`). Weekly volume has its own chain
  without an operation: `savedIds` combined with `currentDateFlow()` →
  `observeTrainings(observationRange(now))` (the current ISO week plus
  `TYPICAL_WEEKS` = 4 complete weeks before it) →
  `WeeklyVolumeUseCase.fromTrainings(trainings, now)` with the same `now` instant
  that resolved the window, carried with the list in a private `WeeklyTrainings`
  carrier; it never reads the Home-period list, so the typical week is never
  computed from a truncated period. The comparison period is the Home period's
  kind anchored at the saved workout's date
  (`DateRangeFormatState.of(kind, anchor = saved.createdAt.date)`, by the owner's
  decision): for a workout saved today it is the Home period itself; an Edit of an
  older workout gets the same kind of period ending on its own day (Last30Days:
  the 30 days up to it; Monthly: its calendar month), so it is compared with what
  preceded it then rather than with today's window. The input is the saved
  workout (from its own observation) against the comparison-period trainings
  ordered before it: `PerformanceTrendUseCase.fromTraining(training,
  periodTrainings, baseline)` keeps those that precede it by `createdAt` then `id`
  and appends it as current, so editing an older workout never borrows a newer
  workout's verdict, growth bars or confetti, in line with the
  PerformanceTrendDetails dialog's chronological statuses. The hero names the
  period by its preset (`range.label()`) only when the comparison period equals
  the Home period, otherwise by its dates (`DateRangeFormatState.short()`).
  Streak and goal progress use the whole Home period list; their "before this workout" side (the saved workout filtered
  out) is computed only for a brand-new workout (`stage.id == null`, Add or
  Draft). Both sides are evaluated at the later of `DateTimeUtils.now()` and the
  saved workout's `createdAt` (server time) in the period list:
  `TrainingStreakUseCase` and `GoalFollowingUseCase` cap their windows at `now`,
  so a device clock lagging the server would otherwise drop the just-saved
  workout from the "after" side; without the saved workout in the list the
  device time stands. For `StageState.Edit` the VM publishes `previousStreak` and
  `previousGoalProgress` as null, so the Growth cards fall back to the plain
  headline/score: an edited workout was already part of the period and a
  "4 → 5" step would be fabricated. The verdict helper reads FirstWorkout
  whenever no voting metric carries a comparison (a percentage against a
  non-zero baseline, or a period best); this has no VM impact beyond
  the confetti check, which fires only for `Record`, and `PerformanceTrendUseCase`
  assigns `Record` only when a previous best (hence an average) exists; with the
  Previous baseline the use case never reports `Record`. A `Record` verdict moves
  `recordCelebration` from `Pending` to `Playing` only; the Screen shows the
  parade once the SaveTraining loader has cleared, and its `onShown` at start
  sets `Done`, which is never re-armed, so a later toggle or recomputation
  cannot replay the parade.
- LoadRecords: `TrainingRecordsUseCase.fromTraining(training)` (backend
  achievements per exercise example of the saved workout) once for the saved
  workout, in its own finite `Flow.safeLaunch(processing = Processing.Infinity)`
  chain (the shared `savedTrainings` source → non-null → `take(1)` →
  `mapLatest(::loadRecords)`), which completes after that one load, so it
  neither stays subscribed nor restarts on pause/resume. `loadRecords` runs the
  operation inside `withLoader(LoadRecords)` (the nesting Home uses) and hands
  the result to `provideRecords`, which maps it to State. A failure shows the shared
  error dialog, the loader clears in `finally` (the Screen then renders no
  records section because `records` is still empty) and nothing navigates Back
  because this chain is separate from the startup `safeLaunch(onError = Back)`.
  The result sets `records` (empty = no section) and a non-empty list moves
  `recordCelebration` from `Pending` to `Playing` only.
- ShareRecap: from `onShareClick(capture)`, after the busy guard and ShareClicked,
  `safeLaunch(loader = ShareRecap)` runs one operation around the capture (the
  Screen-supplied `suspend () -> ImageBitmap`, `heroLayer.toImageBitmap()` of the
  hero layer recorded over the opaque screen background) and
  `ImageSharer.share(image, "grippo-workout.png")`, which returns `Unit` once
  presentation is requested (Android: the chooser is started; iOS:
  `presentViewController` is called); a presentation the platform refuses after
  that is not reported and the operation completes. No result is read and the
  chosen target is never known. Failures thrown by the capture
  (`toImageBitmap()`), PNG encoding, file writing or the presentation request (no
  Android activity or iOS view controller able to show the sheet) end the
  operation as failed and reach the standard error pipeline. The loader keeps one share at a time:
  the overlay's Share button is Loading (not clickable) while
  `TrainingCompletedLoader.ShareRecap` is in `loaders` and Enabled only for a
  `TrainingRecapHeroStyle.Resolved` hero, and a callback arriving while the
  loader is active returns without an event. The bitmap is never archived.

The recording initialization chain runs only when the restored or initial
exercises are null. Continuous catalogs, validation, total calculations, the
body-weight lookup and notification scheduling are not artificial operations; the
exercise editor has no operations. No coroutine, retry, dispatcher, cancellation
guard or business branch exists for telemetry.

Recording exercise totals and Completed save payloads use TrainingTotalUseCase.
Intensity averages nonnegative effective load over every measured repetition,
including fully assisted sets at zero; assistance above body load contributes
zero load without removing its repetitions. Missing/incomplete, conflicting,
negative or nonfinite load components do not contribute tonnage or its intensity
denominator; performed repetitions retain their existing independent count.
Accepted assistance zero remains distinct from missing input through the existing
Exercise → Recording state, domain mappers and durable draft. No event, operation,
route or saved-state field is added. Existing saved totals are not rewritten.
For locally known BodyAndAssist examples requiring assistance, retained sets
without Valid assistance (including after replacing an example) keep their raw
values and have Invalid(null) volume/intensity. Recording propagates unavailable
tonnage to its header and suppresses an apparent maximum load. Save and the final
duration-confirmation callback route the first affected exercise to the existing
editor before completion; this automatic correction does not emit a synthetic
EditExercise click or CompletionOpened/Confirmed. Incomplete drafts remain
persisted for correction. Unknown catalog requirements are not inferred or fetched
by this check. No historical multiplier is substituted.

## Catalog and reporting decision

`TrainingAnalyticsModule` (`@Module @ComponentScan`) binds `TrainingAnalyticsImpl`
as `TrainingAnalytics` and registers one qualified `training_analytics` catalog
with five version-1 schemas: `training_interaction` [training_action] and
`training_task_started` [training_operation], `training_task_completed/failed/interrupted`
[training_operation, duration_ms]; `shared/.../Koin.kt` lists it once. The
exhaustive encoder maps all 41 typed events (Recording 16, Exercise 13,
CompletionConfirmation 4, Completed 8) to closed `training_action` codes:

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
- Completed: `completed_metric_opened`, `completed_streak_opened`,
  `completed_goal_opened`, `completed_add_goal_clicked` (typed event
  AddGoalClicked, the goal dialog's add-goal action),
  `completed_baseline_selected`, `completed_record_opened`,
  `completed_share_clicked`, `completed_dismiss_requested`.
- Operation codes: `delete_draft`, `load_draft`, `load_training`, `save_draft`,
  `create_training`, `update_training`, `refresh_examples`,
  `refresh_trainings`, `load_records`, `share_recap`; all ten have VM callers,
  and every event variant has at least one caller.

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
events do not establish the corrected current-build outcome. The current
contract is the 41 event codes above, eight of them Completed, and the ten
operation codes, carried by the existing `training_interaction` and four
`training_task_*` schemas without a schema name,
parameter key or version of their own. `completed_exercise_opened` is in neither
list: Completed has no exercise list, so the code was deleted, and the typed
catalog, not yet in a store build, keeps no compatibility alias for it. `completed_summary_opened` is absent
too: by the user's decision Completed no longer has the hero's Statistics
button, so `onSummaryClick`, `TrainingEvent.Completed.SummaryOpened` and
`InteractionAction.CompletedSummaryOpened` were deleted with the code. Reporting
decision: the observation ends without a replacement; `training_interaction`
keeps its name, its `training_action` key and version 1 (typed catalog not yet in a
store build), no alias is kept, and earlier test-build events carrying the code remain history
only. The Statistics dialog's own `statistics_*` facts are unchanged; that
dialog is now opened only from Trainings.
Decorators, consent and transport are unchanged. Live admin reconciliation of
these codes, including both removals, is recorded in the root review table, not here.

## Omissions and privacy

No raw weights, repetitions, duration or date values, exercise names, entity IDs,
input lengths, credentials, error messages, URLs or bodies enter reporting.
Dialog presentation/close/dismiss/cancel outcomes (StartTraining, Confirmation,
ConfirmTrainingCompletion, Iteration, ExerciseExampleDetails,
PerformanceTrendDetails, TrainingStreakDetails, TrainingGoalDetails),
the tapped metric `type` passed to Completed's `onMetricClick` (MetricOpened is
parameterless; the type is a business dialog input, not encoded), the selected
baseline value (BaselineSelected is parameterless; Average/Previous is a business
State value, not encoded), the kind of the tapped record and its exercise example
(RecordOpened is parameterless; the ID is a dialog input), the share target
(the toolkit returns `Unit` once presentation is requested, so the chosen target
is never known or encoded; ShareRecap reports only that the capture and the complete
toolkit call completed, failed or was interrupted), the
records load outcome beyond its operation,
metric-row identity inside PerformanceTrendDetails, period-metric computation
and the Completed period refresh outcome beyond its operation, the Completed
confetti presentation and its `onRecordConfettiShown` acknowledgement (decorative,
no user action), scrim/swipe dismissal, internal unaccepted picker edits (the nested DurationPicker
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

Known limitation: the Completed streak and goal cards are evaluated at the later
of the device time and the saved workout's `createdAt`, while
TrainingStreakDetails and TrainingGoalDetails, opened from those cards with only
the period range, evaluate at the device time. With a device clock behind the
server, the dialog can therefore leave out the workout the card already counts.
`DialogConfig` carries no evaluation instant and is not changed for this.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| Recording | `stage` | Preserve | `DRAFT = PersistenceKey("TrainingRecordingDraft", TrainingRecordingState.serializer())`; nullable initialState; `saveState` of the latest VM state | The route stage is used only without an archive; Draft loading rewrites the stage to Add or Edit(trainingId), and that resolved value is archived |
| Recording | `exercises` (nullable immutable list) including iteration values with pending/empty formats, IDs, order, example identity, createdAt and totals | Preserve | ImmutableListSerializer; serializable ExerciseState/IterationState | Null means the initial source load is pending, a nonnull empty list is a real empty draft; the initialization chain runs only when restored exercises are null (Add also re-runs DeleteDraft and may reopen the Start dialog); providers return early once nonnull |
| Recording | `startAt` | Preserve | Required field, no default | `DateTimeUtils.now()` only without an archive; Edit/Draft loading sets now minus duration; completion confirmation sets now minus the confirmed duration; the ticking text is rebuilt in the Screen |
| Recording | `examples`, `muscles` | Rebuild | `@Transient`; example observation by current example IDs, muscle observation | Local example emissions recalculate derived totals, retaining every set and its recorded fields. A known required-assistance set without Valid assistance has unavailable load/intensity until corrected; saved names and raw iterations remain unchanged. Cards resolve the displayed nested example name from this catalog, with the draft label as offline/missing fallback |
| Recording | Room draft training (data layer) | Rebuild from the durable source | `setDraftTraining` after each accepted mutation, `deleteDraftTraining` on empty/discard/completion cleanup, `getDraftTraining` for the Draft stage | The draft deeplink and Main's Continue re-enter through `StageState.Draft` |
| Recording | Reminder notification; StartTraining / Confirmation / ConfirmTrainingCompletion sessions | Runtime only | NotificationManager; live DialogConfig callbacks | Accepted results are applied to State before navigation; sessions are unsaved |
| Recording | Accepted library result; accepted editor Sync/Remove | Preserve at parent | Result keys `exercise-library-add` and `exercise`; the root picker sends the accepted example to the retained Recording receiver, which seeds the local editor route; Sync/Remove update the parent and save the draft | Re-entry seeds the editor from the parent's accepted exercise; a popped editor archive is discarded with its child context |
| Exercise | `exercise` (ExerciseState) | Preserve | `DRAFT = PersistenceKey("TrainingExerciseDraft", TrainingExerciseState.serializer())`; nullable initialState | The route seed is used only without an archive, so the local draft wins; the exercise ID never changes (a swap copies name/example), keeping the SimilarTo result key stable |
| Exercise | `exerciseExample`, `previousIterations`, volume/repetition artifact IDs | Rebuild | `@Transient`; example observation by current ID; validation flow combining the current exercise with the observed context | Recomputed from the restored exercise without replaying callbacks; findings reset only when the observed context belongs to another example or date, when the exercise has no creation date to observe a context for, or after an error dismissal, so the WhileActive resubscription after pause/resume re-emits the same context without blanking the previous-set hint |
| Exercise | Iteration dialog session; body-weight lookup | Runtime only | DialogConfig.Iteration; WeightHistoryFeature | The accepted iteration enters the exercise and reaches the parent through Sync/Remove |
| Completed | `savedTrainingId` | Preserve | `DRAFT = PersistenceKey("TrainingCompletedDraft", TrainingCompletedState.serializer())`; `restoreState(DRAFT) ?: TrainingCompletedState()` (non-null); default null | Committed only after successful required training readback, hydration and UseCase user refresh; a restored ID skips Create/Update and resumes DeleteDraft/LoadTraining/RefreshExamples/LoadRecords and presentation. Repository failures keep this checkpoint uncommitted and preserve the durable draft |
| Completed | `baseline` (PerformanceBaselineState) | Preserve | Same DRAFT archive; `@Serializable` enum, default `Average`; committed by `onBaselineSelect` through `update` | The user's selection, archived as selected (not the computed baseline inside `performance`): the restored value feeds the period chain (the private `observeBaseline()` flow, distinct) so the comparison recomputes with the saved baseline; archives written before the field existed decode with the `Average` default (kotlinx default for a missing property) |
| Completed | `records` (immutable list, default empty) | Rebuild | `@Transient`, default `persistentListOf()`; the first non-null emission of the shared `savedTrainings` source (`take(1)`) → `loadRecords`: `TrainingRecordsUseCase.fromTraining` (backend achievements) in the LoadRecords operation under the `LoadRecords` loader → `provideRecords` (`toState()`), in a finite `Processing.Infinity` chain (one load for the saved ID, then the chain completes; no restart on resume) | The Screen renders the records skeleton from the `LoadRecords` loader, the card when the list is non-empty and nothing otherwise; a failed load clears the loader and leaves the list empty (no section); recomputed after restoration from the saved ID; never archived (backend-owned facts) |
| Completed | `weeklyVolume` | Rebuild | `@Transient`, default null; its own chain: `savedIds` combined with `currentDateFlow()` → `observeTrainings(WeeklyVolumeUseCase.observationRange(now))` (current ISO week + `TYPICAL_WEEKS` complete weeks) → `WeeklyVolumeUseCase.fromTrainings(trainings, now)` with the window's own `now`, one instant carried with the list in `WeeklyTrainings`; the cache is refreshed by the RefreshTrainings union request | Null renders the weekly volume skeleton until the window is computed; recomputed with each window emission and at day rollover; independent of the Home period |
| Completed | `training`, `muscleLoad` | Rebuild | `@Transient`; continuous `observeTraining(savedTrainingId)` plus the distribution use case | Details rebuild by ID and follow Room catalog updates, including nested localized exercise names; the hero's verdict and line come from `performance` through `PerformanceVerdictState.summary`, and the Screen only joins the period label (the Home period's name when the comparison period is the Home period, otherwise the comparison period's dates); neither is stored. Startup LoadTraining/RefreshExamples operation boundaries and the saved ID checkpoint remain; no localized snapshot is archived |
| Completed | `range` (DateRangeFormatState) | Rebuild from the durable settings source | `@Transient`, default `Empty()`; `LocalSettingsFeature.observeRange()` (the settings repository returns the stored range or its `DefaultRange`, Last30Days, so neither ViewModel keeps a fallback; Home keeps its own pre-emission placeholder) combined with `currentDateFlow()` (day rollover re-resolves the preset, as on Home) → `Range.toState()` → `DateRangeFormatState.of(kind)` | The committed Home period is the single owner; no StateKeeper copy; `range.value` stays null until the settings emission, which also gates every period flow and the dialog callbacks |
| Completed | `performance` (`PerformanceComparisonState?`: the five computed metrics, of which the card renders all but Duration, the baseline they were computed against and their comparison period) | Rebuild | `@Transient`, default null; `observeTrainings(period)` for the `PeriodBaseline` emission (the saved training from its own observation, the Home period kind anchored at its date and the selected baseline), observed per saved training; `PerformanceTrendUseCase.fromTraining(training = saved, periodTrainings = trainings, baseline)`. Input: the saved workout against the comparison-period trainings before it (createdAt then id, inside the use case) | Recomputed after restoration from the saved ID, the current period and the restored selection; null means "not yet computed" (a computed comparison always has five metrics; it waits for the saved training to load and compares it with the period trainings before it) and renders the hero in `TrainingRecapHeroStyle.Pending` (the facts stay visible and the overlay's Share stays Disabled) plus the comparison skeleton. `provideComparison` sets the metrics, their baseline and period in one update, so after a tap the hero line, chips and segment keep the previous computed baseline until the recomputed metrics arrive and switch with them; no counts or flags beside these sources |
| Completed | `streak`, `goalProgress` | Rebuild | `@Transient`, default null; the streak's own Home-period `observeTrainings` chain (`PeriodStreak`), and the Home-period trainings combined with `observeExerciseExamples(ids)` and `observeGoal()` into `PeriodGoal` for the goal, gated on the restored `savedTrainingId` and the resolved period; `TrainingStreakUseCase` and `GoalFollowingUseCase`, evaluated at the later of `DateTimeUtils.now()` and the saved workout's `createdAt` | Recomputed after restoration from the saved ID and the current period; null hides the card (no training in the period, and for the goal also no goal) |
| Completed | `previousStreak`, `previousGoalProgress` | Rebuild | `@Transient`, default null; for a brand-new workout (route `stage.id == null`) the same period observations with the saved training filtered out (`id != savedTrainingId`), evaluated at the same time as the current side; null when no other training remains in the period, when there is no goal, or always for `StageState.Edit` | The "before this workout" side of the Growth cards; recomputed with the current values from one emission, never archived or cached separately; the Edit null is derived from the retained route stage, not stored |
| Completed | `recordCelebration` (`RecordCelebrationState`: Pending / Playing / Done) | Runtime only | `@Transient`, default `Pending`; a `PerformanceVerdictState.Record` verdict or a non-empty records load moves `Pending → Playing` through `celebrate(isRecord)`; the Screen shows the parade only while `Playing` and after the SaveTraining loader clears, so it plays once over the content, never over the saving skeleton; `onRecordConfettiShown` (reported by the parade as it starts) sets `Done`; `Playing` and `Done` never re-arm | Not archived: a retained VM (rotation, theme) never replays the parade because the state is already `Done`, and later recomputations/readbacks cannot retrigger it; after process death the fresh VM starts `Pending`, so its computed period or records load can celebrate again once (deliberate: the one-shot state is presentation, not a business draft) |
| Completed | Share capture (`GraphicsLayer`, capture lambda, `ImageBitmap`) | Runtime only | Screen-level `rememberGraphicsLayer()` (`heroLayer` in `TrainingCompletedScreen`, shared by the hero that records it over the opaque `background.screen` base and the overlay's Share button); each tap scrolls the content list back to the hero, settles its backdrop, waits until the settled hero has been drawn and then passes `{ heroLayer.toImageBitmap() }` to `onShareClick`, and the bitmap exists only inside the ShareRecap operation that invokes it; Share is Enabled only once the SaveTraining loader has cleared, `performance` is computed and the `ShareRecap` loader is absent | Presentation state: a recomposed or recreated hero records a fresh frame that the next tap captures (a hero that has left the lazy list is recomposed by that scroll); a restored screen keeps Share disabled until its period recomputes; nothing about the image or the share sheet is archived or restored |
| Completed | Card entrance and scroll state: the screen-level `rememberTrainingRecapHeroState()` (the hero backdrop's 900 ms reveal, saved as one Boolean once the reveal has started, plus the generation that drops a running 250 ms verdict crossfade on settle) and the `rememberSaveable` reveal flags of `PerformanceComparisonCard` (growth bars), the Growth `TrainingStreakCard` (streak length count-up) and the Growth `GoalCard` (score count-up); the screen-level `rememberLazyListState()` of the content list (Share scrolls it back to the hero) and the skeleton list's own default state; the `KonfettiParade` local state | Runtime only (presentation widget state) | Compose saveable state: the hero state at the screen level (its own `Saver`, so it outlives the hero item), each card item's flag in the lazy content list (kept per item key while the item is out of view), plus the saveable list states, no StateKeeper key; the parade uses plain `remember` | Not a business draft and not in the VM: an ordinary recreation keeps a played entrance from replaying and keeps the scroll offset; a fresh screen instance plays each entrance once (the backdrop reveal runs when the Resolved backdrop first composes; a lower card plays its entrance when it first scrolls into view) |
| Completed | Route inputs `stage`, `exercises`, `startAt` | Rebuild from route | `TrainingRouter.Completed` constructor inputs | `exercises` and `startAt` serve only the save branch (duration is computed at write time, so a process death during the save restores the route and saves again with the downtime added, while the server may already hold the first write: a known limitation until the route carries the confirmed duration); `stage` selects create versus update and, as the retained `private val`, whether the Growth "before" side is computed at all (`stage.id == null`); no stage-dependent copy is stored, the hero verdict comes from the computed period |
| Completed | SaveTraining, LoadRecords and ShareRecap loaders; RefreshTrainings, RefreshExamples and ShareRecap jobs; ExerciseExampleDetails/PerformanceTrendDetails/TrainingStreakDetails/TrainingGoalDetails dialogs; Goal direction | Runtime only | VM loaders, nested/standalone `safeLaunch`, DialogConfig, `TrainingCompletedDirection.Goal` | Not archived; the refresh and the records load restart from the restored ID and period; RefreshTrainings (WhileActive) also repeats after a resume beyond the one-second disconnect grace, the records load (a finite `take(1)` chain under Processing.Infinity) does not; an in-flight share is not resumed, and a fresh VM starts without the ShareRecap loader; dialogs are unsaved sessions |
| Training container | Child stack: Recording / Exercise(seed, result key) / Completed(stage, exercises, startAt) | Preserve (navigation) | `TrainingRouter.serializer()`, key `TrainingComponent`; `RootRouter.Training(stage)` preserved by the root stack | The parent re-registers its result observers in init; route inputs remain ordinary business inputs; no duplicate container draft |
| Root container / external exercise picker | `RootRouter.Exercises(Library(mode))`, including the mode's `resultKey`, above the retained Training route | Preserve (navigation) | `RootRouter.serializer()`, key `RootComponent` | Same-build restoration rebuilds the Training receiver and picker with the original mode/result key; durable workout drafts are not cleared |
| Root container / Goal screen | `RootRouter.Profile(ProfileRouter.Goal)` pushed by the goal dialog's add-goal action above the retained `RootRouter.Training`, whose own stack holds `Completed(stage, exercises, startAt)` | Preserve (navigation) | `RootRouter.serializer()`, key `RootComponent`; `TrainingRouter.serializer()`, key `TrainingComponent` | Same-build restoration rebuilds the Goal screen on top and Training with Completed beneath it; Completed restores its own DRAFT (`savedTrainingId` skips Create/Update, `baseline` keeps the selection) and its WhileActive chains run once it is resumed; Back at the Profile root (`RootViewModel.onBack`) pops to Completed |
| Training container | `TrainingState.exercises` | Runtime only (never written) | Container state without `update` calls | No DRAFT; the container holds no business draft |
| ConfirmTrainingCompletion dialog | `duration` (DurationFormatState), including the accepted nested-picker value before Confirm | Runtime only (explicit user decision: the existing unsaved-dialog policy) | State not serializable; retainedInstance; host session/step serializers null; no DRAFT | Initial from the parent's elapsed time since startAt, coerced to at least one minute; ordinary recreation keeps the live VM; process death closes both dialog steps |
| ConfirmTrainingCompletion dialog | `exercises` snapshot and its derived recorded exercise/set counts | Runtime only | Recording State → live DialogConfig → retained dialog VM State; counts use `!isPending`, matching completion cleanup | Snapshot is read-only and does not modify the recording; re-entry receives current parent exercises; nested duration edits retain it; process death closes the session while Recording preserves its own exercises |
| ConfirmTrainingCompletion → Recording → Completed | Confirmed duration | Preserve at parent, then route | Recording sets `startAt = now - result` and replaces the stack with `Completed(stage, exercises, startAt)` | Recording's archive is dropped by `replaceAll`; accepted values live in the Completed route |

The exact `@Transient` sets are Recording (examples, muscles), Exercise (example,
previous iterations, two artifact lists) and Completed (training, muscleLoad,
range, performance, records, weeklyVolume, streak, previousStreak, goalProgress,
previousGoalProgress, recordCelebration); Completed archives `savedTrainingId`
and `baseline`. No training ViewModel holds a `var` or `lateinit` member; eligibility
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
