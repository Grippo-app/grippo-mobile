package com.grippo.training.recording

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogController
import com.grippo.state.trainings.stubExercises

internal class TrainingRecordingViewModel(
    private val dialogController: DialogController,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState()
), TrainingRecordingContract {

    init {
        safeLaunch {
            update { it.copy(exercises = stubExercises()) }
        }
    }

    override fun onAddExercise() {
        navigateTo(TrainingRecordingDirection.ToExercise(null))
    }

    override fun onEditExercise(id: String) {
        navigateTo(TrainingRecordingDirection.ToExercise(id))
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