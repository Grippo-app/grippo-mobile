package com.grippo.training.recording

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogController
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.persistentListOf
import kotlin.uuid.Uuid

internal class TrainingRecordingViewModel(
    private val dialogController: DialogController,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState()
), TrainingRecordingContract {

    override fun onAddExercise() {
        val exercise = ExerciseState(
            id = Uuid.random().toString(),
            name = "Test name",
            volume = VolumeFormatState.of(0f),
            repetitions = RepetitionsFormatState.of(0),
            intensity = IntensityFormatState.of(0f),
            iterations = persistentListOf(),
            exerciseExample = null
        )
        navigateTo(TrainingRecordingDirection.ToExercise(exercise))
    }

    override fun onEditExercise(id: String) {
        val exercise = state.value.exercises.find { it.id == id } ?: return
        navigateTo(TrainingRecordingDirection.ToExercise(exercise))
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