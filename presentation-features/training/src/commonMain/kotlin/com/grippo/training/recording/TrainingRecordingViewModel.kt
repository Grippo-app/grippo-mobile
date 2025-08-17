package com.grippo.training.recording

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class TrainingRecordingViewModel(
    private val dialogController: DialogController,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState()
), TrainingRecordingContract {

    override fun onAddExercise() {
        dialogController.show(
            DialogConfig.ExerciseExamples(
                onResult = { id ->
                    // TODO: add exercise by id
                }
            )
        )
    }

    override fun onOpenFilters() {
        dialogController.show(
            DialogConfig.Filters(
                onResult = { /* handle filters */ }
            )
        )
    }

    override fun onOpenExerciseExample(id: String) {
        dialogController.show(DialogConfig.ExerciseExample(id = id))
    }

    override fun onAddIteration(exerciseId: String) {
        dialogController.show(
            DialogConfig.Iteration(
                weight = 20f,
                repeats = 5,
                onResult = { weight, repeats ->
                    // TODO: add iteration
                }
            )
        )
    }

    override fun onEditIteration(exerciseId: String, iterationId: String) {
        dialogController.show(
            DialogConfig.Iteration(
                weight = 20f,
                repeats = 5,
                onResult = { weight, repeats ->
                    // TODO: update iteration
                }
            )
        )
    }

    override fun onRemoveIteration(exerciseId: String, iterationId: String) {
        // TODO: implement remove
    }

    override fun onOpenStats() {
        update { it.copy(tab = RecordingTab.Stats) }
    }

    override fun onOpenExercises() {
        update { it.copy(tab = RecordingTab.Exercises) }
    }

    override fun onSave() {
        navigateTo(TrainingRecordingDirection.ToSuccess)
    }

    override fun onBack() {
        navigateTo(TrainingRecordingDirection.Back)
    }
}