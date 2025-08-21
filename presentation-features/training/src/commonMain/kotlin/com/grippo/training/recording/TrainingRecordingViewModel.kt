package com.grippo.training.recording

import com.grippo.core.BaseViewModel
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.persistentListOf
import kotlin.uuid.Uuid

internal class TrainingRecordingViewModel(
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
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
        safeLaunch {
            val dialog = DialogConfig.Confirmation(
                title = stringProvider.get(Res.string.training_progress_lost_title),
                description = stringProvider.get(Res.string.training_progress_lost_description),
                onResult = { navigateTo(TrainingRecordingDirection.Back) }
            )

            dialogController.show(dialog)
        }
    }
}