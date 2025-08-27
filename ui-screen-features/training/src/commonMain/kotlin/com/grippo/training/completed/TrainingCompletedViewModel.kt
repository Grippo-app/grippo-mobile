package com.grippo.training.completed

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.toTrainingListValues
import com.grippo.state.domain.training.toDomain
import com.grippo.state.trainings.TrainingState
import kotlinx.coroutines.flow.firstOrNull

internal class TrainingCompletedViewModel(
    training: TrainingState,
    trainingFeature: TrainingFeature,
    private val dialogController: DialogController
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    TrainingCompletedState()
), TrainingCompletedContract {

    init {
        safeLaunch(loader = TrainingCompletedLoader.SaveTraining) {
            val id = trainingFeature
                .setTraining(training.toDomain())
                .getOrThrow() ?: return@safeLaunch

            val domain = trainingFeature.observeTraining(id).firstOrNull()
            provideTraining(domain)
        }
    }

    private fun provideTraining(value: Training?) {
        val training = value?.toState()?.toTrainingListValues() ?: return
        update { it.copy(training = training) }
    }

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(
            id = id,
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(TrainingCompletedDirection.Back)
    }
}
