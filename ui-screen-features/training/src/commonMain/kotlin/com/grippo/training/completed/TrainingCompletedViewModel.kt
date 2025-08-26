package com.grippo.training.completed

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.training.toDomain
import com.grippo.state.trainings.TrainingState
import kotlinx.coroutines.flow.firstOrNull

internal class TrainingCompletedViewModel(
    training: TrainingState,
    trainingFeature: TrainingFeature
) :
    BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
        TrainingCompletedState()
    ), TrainingCompletedContract {


    init {
        safeLaunch(loader = TrainingCompletedLoader.SaveTraining) {
            val id = trainingFeature
                .setTraining(training.toDomain())
                .getOrThrow()

            val training = trainingFeature.observeTraining(id).firstOrNull()

            provideTraining(training)
        }
    }

    private fun provideTraining(value: Training?) {
        update { it.copy(training = value?.toState()) }
    }

    override fun onBack() {
        navigateTo(TrainingCompletedDirection.Back)
    }
}
