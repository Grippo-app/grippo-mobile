package com.grippo.training

import com.grippo.core.BaseViewModel
import com.grippo.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime
import com.grippo.training.TrainingState as ScreenTrainingState

public class TrainingViewModel :
    BaseViewModel<ScreenTrainingState, TrainingDirection, TrainingLoader>(ScreenTrainingState()),
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

    override fun toCompleted(exercises: List<ExerciseState>, startAt: LocalDateTime) {
        navigateTo(TrainingDirection.ToCompleted(exercises, startAt))
    }
}