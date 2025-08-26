package com.grippo.training

import com.grippo.core.BaseViewModel
import com.grippo.state.trainings.ExerciseState

public class TrainingViewModel :
    BaseViewModel<TrainingState, TrainingDirection, TrainingLoader>(TrainingState()),
    TrainingContract {

    override fun onBack() {
        navigateTo(TrainingDirection.Back)
    }

    override fun onClose() {
        navigateTo(TrainingDirection.Close)
    }

    override fun toRecording() {
        navigateTo(TrainingDirection.ToRecording)
    }

    override fun toExercise(exercise: ExerciseState) {
        navigateTo(TrainingDirection.ToExercise(exercise))
    }

    override fun toCompleted(exercises: List<ExerciseState>) {
        navigateTo(TrainingDirection.ToCompleted(exercises))
    }
}