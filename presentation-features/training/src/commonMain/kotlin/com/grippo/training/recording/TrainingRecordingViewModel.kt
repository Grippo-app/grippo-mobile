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
        // TODO REMOVE IT
        val dialog = DialogConfig.Iteration(
            volume = 20f,
            repeats = 5,
            onResult = { volume, repeats ->
                // TODO: update iteration
            }
        )

        dialogController.show(dialog)
    }

    override fun onOpenFilters() {
    }

    override fun onOpenExerciseExample(id: String) {
        dialogController.show(DialogConfig.ExerciseExample(id = id))
    }

    override fun onAddIteration(exerciseId: String) {
        val dialog = DialogConfig.Iteration(
            volume = 20f,
            repeats = 5,
            onResult = { volume, repeats ->
                // TODO: update iteration
            }
        )

        dialogController.show(dialog)
    }

    override fun onEditIteration(exerciseId: String, iterationId: String) {
        val dialog = DialogConfig.Iteration(
            volume = 20f,
            repeats = 5,
            onResult = { volume, repeats ->
                // TODO: update iteration
            }
        )

        dialogController.show(dialog)
    }

    override fun onRemoveIteration(exerciseId: String, iterationId: String) {
        // TODO: implement remove
    }

    override fun onSelectTab(tab: RecordingTab) {
        update { it.copy(tab = tab) }
    }

    override fun onSave() {
        navigateTo(TrainingRecordingDirection.ToSuccess)
    }

    override fun onBack() {
        navigateTo(TrainingRecordingDirection.Back)
    }
}