package com.grippo.analytics.training.internal.schema

import com.grippo.analytics.training.internal.events.TrainingPayloadEvent
import com.grippo.analytics.training.internal.models.InteractionAction
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object TrainingEventMatrix {
    private val Action = AnalyticsParameterKey.code<InteractionAction>("training_action", encode = {
        when (it) {
            InteractionAction.RecordingAddExercise -> "recording_add_exercise"
            InteractionAction.RecordingEditExercise -> "recording_edit_exercise"
            InteractionAction.RecordingDeleteExercise -> "recording_delete_exercise"
            InteractionAction.RecordingReorderExercises -> "recording_reorder_exercises"
            InteractionAction.RecordingSave -> "recording_save"
            InteractionAction.RecordingDismissRequested -> "recording_dismiss_requested"
            InteractionAction.RecordingStartOpened -> "recording_start_opened"
            InteractionAction.RecordingStartConfirmed -> "recording_start_confirmed"
            InteractionAction.RecordingLibrarySelected -> "recording_library_selected"
            InteractionAction.RecordingExerciseUpdated -> "recording_exercise_updated"
            InteractionAction.RecordingUnfinishedOpened -> "recording_unfinished_opened"
            InteractionAction.RecordingUnfinishedConfirmed -> "recording_unfinished_confirmed"
            InteractionAction.RecordingCompletionOpened -> "recording_completion_opened"
            InteractionAction.RecordingCompletionConfirmed -> "recording_completion_confirmed"
            InteractionAction.RecordingDiscardOpened -> "recording_discard_opened"
            InteractionAction.RecordingDiscardConfirmed -> "recording_discard_confirmed"
            InteractionAction.ExerciseAddIteration -> "exercise_add_iteration"
            InteractionAction.ExerciseEditVolume -> "exercise_edit_volume"
            InteractionAction.ExerciseEditRepetitions -> "exercise_edit_repetitions"
            InteractionAction.ExerciseDeleteIteration -> "exercise_delete_iteration"
            InteractionAction.ExerciseReorderIterations -> "exercise_reorder_iterations"
            InteractionAction.ExerciseReorderFinished -> "exercise_reorder_finished"
            InteractionAction.ExerciseExampleOpened -> "exercise_example_opened"
            InteractionAction.ExerciseReplaceExample -> "exercise_replace_example"
            InteractionAction.ExerciseExampleSelected -> "exercise_example_selected"
            InteractionAction.ExerciseSave -> "exercise_save"
            InteractionAction.ExerciseDismissRequested -> "exercise_dismiss_requested"
            InteractionAction.ExerciseIterationOpened -> "exercise_iteration_opened"
            InteractionAction.ExerciseIterationConfirmed -> "exercise_iteration_confirmed"
            InteractionAction.CompletionConfirmationDurationOpened -> "completion_confirmation_duration_opened"
            InteractionAction.CompletionConfirmationDurationSelected -> "completion_confirmation_duration_selected"
            InteractionAction.CompletionConfirmationConfirm -> "completion_confirmation_confirm"
            InteractionAction.CompletionConfirmationDismissRequested -> "completion_confirmation_dismiss_requested"
            InteractionAction.CompletedSummaryOpened -> "completed_summary_opened"
            InteractionAction.CompletedExerciseOpened -> "completed_exercise_opened"
            InteractionAction.CompletedDismissRequested -> "completed_dismiss_requested"
        }
    })
    private val Interaction = AnalyticsEventSchema("training_interaction", 1, listOf(Action))
    val all = listOf(Interaction)

    private fun action(event: TrainingEvent): InteractionAction = when (event) {
        TrainingEvent.Recording.AddExercise -> InteractionAction.RecordingAddExercise
        TrainingEvent.Recording.EditExercise -> InteractionAction.RecordingEditExercise
        TrainingEvent.Recording.DeleteExercise -> InteractionAction.RecordingDeleteExercise
        TrainingEvent.Recording.ReorderExercises -> InteractionAction.RecordingReorderExercises
        TrainingEvent.Recording.Save -> InteractionAction.RecordingSave
        TrainingEvent.Recording.DismissRequested -> InteractionAction.RecordingDismissRequested
        TrainingEvent.Recording.StartOpened -> InteractionAction.RecordingStartOpened
        TrainingEvent.Recording.StartConfirmed -> InteractionAction.RecordingStartConfirmed
        TrainingEvent.Recording.LibrarySelected -> InteractionAction.RecordingLibrarySelected
        TrainingEvent.Recording.ExerciseUpdated -> InteractionAction.RecordingExerciseUpdated
        TrainingEvent.Recording.UnfinishedOpened -> InteractionAction.RecordingUnfinishedOpened
        TrainingEvent.Recording.UnfinishedConfirmed -> InteractionAction.RecordingUnfinishedConfirmed
        TrainingEvent.Recording.CompletionOpened -> InteractionAction.RecordingCompletionOpened
        TrainingEvent.Recording.CompletionConfirmed -> InteractionAction.RecordingCompletionConfirmed
        TrainingEvent.Recording.DiscardOpened -> InteractionAction.RecordingDiscardOpened
        TrainingEvent.Recording.DiscardConfirmed -> InteractionAction.RecordingDiscardConfirmed
        TrainingEvent.Exercise.AddIteration -> InteractionAction.ExerciseAddIteration
        TrainingEvent.Exercise.EditVolume -> InteractionAction.ExerciseEditVolume
        TrainingEvent.Exercise.EditRepetitions -> InteractionAction.ExerciseEditRepetitions
        TrainingEvent.Exercise.DeleteIteration -> InteractionAction.ExerciseDeleteIteration
        TrainingEvent.Exercise.ReorderIterations -> InteractionAction.ExerciseReorderIterations
        TrainingEvent.Exercise.ReorderFinished -> InteractionAction.ExerciseReorderFinished
        TrainingEvent.Exercise.ExampleOpened -> InteractionAction.ExerciseExampleOpened
        TrainingEvent.Exercise.ReplaceExample -> InteractionAction.ExerciseReplaceExample
        TrainingEvent.Exercise.ExampleSelected -> InteractionAction.ExerciseExampleSelected
        TrainingEvent.Exercise.Save -> InteractionAction.ExerciseSave
        TrainingEvent.Exercise.DismissRequested -> InteractionAction.ExerciseDismissRequested
        TrainingEvent.Exercise.IterationOpened -> InteractionAction.ExerciseIterationOpened
        TrainingEvent.Exercise.IterationConfirmed -> InteractionAction.ExerciseIterationConfirmed
        TrainingEvent.CompletionConfirmation.DurationOpened -> InteractionAction.CompletionConfirmationDurationOpened
        TrainingEvent.CompletionConfirmation.DurationSelected -> InteractionAction.CompletionConfirmationDurationSelected
        TrainingEvent.CompletionConfirmation.ConfirmClicked -> InteractionAction.CompletionConfirmationConfirm
        TrainingEvent.CompletionConfirmation.DismissRequested -> InteractionAction.CompletionConfirmationDismissRequested
        TrainingEvent.Completed.SummaryOpened -> InteractionAction.CompletedSummaryOpened
        TrainingEvent.Completed.ExerciseOpened -> InteractionAction.CompletedExerciseOpened
        TrainingEvent.Completed.DismissRequested -> InteractionAction.CompletedDismissRequested
    }

    fun event(event: TrainingEvent): AnalyticsEvent = TrainingPayloadEvent(
        Interaction, listOf(Action.value(action(event))),
    )
}
