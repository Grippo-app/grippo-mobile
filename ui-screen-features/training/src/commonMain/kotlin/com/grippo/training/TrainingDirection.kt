package com.grippo.training

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import kotlinx.datetime.LocalDateTime

public sealed interface TrainingDirection : BaseDirection {
    public data class ToLibrary(
        val route: ExercisesRouter.Library
    ) : TrainingDirection

    public data object Close : TrainingDirection

    public data object Back : TrainingDirection

    public data class ToRecording(
        val stage: StageState,
    ) : TrainingDirection

    public data class ToExercise(
        val exercise: ExerciseState,
        val resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>,
    ) : TrainingDirection

    public data class ToCompleted(
        val stage: StageState,
        val exercises: List<ExerciseState>,
        val startAt: LocalDateTime,
    ) : TrainingDirection
}
