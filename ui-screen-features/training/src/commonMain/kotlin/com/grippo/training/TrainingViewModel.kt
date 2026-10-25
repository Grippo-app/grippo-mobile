package com.grippo.training

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import com.grippo.training.TrainingState as ScreenTrainingState
import kotlinx.datetime.LocalDateTime

public class TrainingViewModel :
    BaseViewModel<ScreenTrainingState, TrainingDirection, TrainingLoader>(ScreenTrainingState()),
    TrainingContract {

    override fun toLibrary(route: ExercisesRouter.Library) {
        navigateTo(TrainingDirection.ToLibrary(route))
    }

    override fun onBack() {
        navigateTo(TrainingDirection.Back)
    }

    override fun onClose() {
        navigateTo(TrainingDirection.Close)
    }

    override fun toRecording(stage: StageState) {
        navigateTo(TrainingDirection.ToRecording(stage))
    }

    override fun toExercise(exercise: ExerciseState, resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>) {
        navigateTo(TrainingDirection.ToExercise(exercise, resultKey))
    }

    override fun toCompleted(
        stage: StageState,
        exercises: List<ExerciseState>,
        startAt: LocalDateTime
    ) {
        navigateTo(TrainingDirection.ToCompleted(stage, exercises, startAt))
    }

    override fun toGoal() {
        navigateTo(TrainingDirection.ToGoal)
    }
}
