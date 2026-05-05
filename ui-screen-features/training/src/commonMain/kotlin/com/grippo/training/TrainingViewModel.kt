package com.grippo.training

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.stage.TrainingSeed
import com.grippo.core.state.trainings.ExerciseState
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

    override fun toRecording(stage: StageState, seed: TrainingSeed) {
        navigateTo(TrainingDirection.ToRecording(stage, seed))
    }

    override fun toExercise(exercise: ExerciseState) {
        navigateTo(TrainingDirection.ToExercise(exercise))
    }

    override fun toCompleted(
        stage: StageState,
        exercises: List<ExerciseState>,
        startAt: LocalDateTime
    ) {
        navigateTo(TrainingDirection.ToCompleted(stage, exercises, startAt))
    }
}