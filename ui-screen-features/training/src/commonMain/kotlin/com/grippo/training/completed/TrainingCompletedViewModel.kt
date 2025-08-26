package com.grippo.training.completed

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.state.trainings.ExerciseState

internal class TrainingCompletedViewModel(
    exercises: List<ExerciseState>,
    trainingFeature: TrainingFeature
) :
    BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
        TrainingCompletedState()
    ), TrainingCompletedContract {


    init {
        safeLaunch(loader = TrainingCompletedLoader.SaveTraining) {
//            val id = trainingFeature.setTraining(training).getOrThrow() ?: return@safeLaunch
//
//            val trainingState = trainingFeature.observeTraining(id).firstOrNull()
//
//            update { it.copy(training = trainingState?.toState()) }
        }
    }

    override fun onBack() {
        navigateTo(TrainingCompletedDirection.Back)
    }
}
