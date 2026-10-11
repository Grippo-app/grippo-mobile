package com.grippo.training.recording

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import kotlinx.datetime.LocalDateTime

internal sealed interface TrainingRecordingDirection : BaseDirection {
    data class ToLibrary(
        val route: ExercisesRouter.Library
    ) : TrainingRecordingDirection

    data class ToCompleted(
        val stage: StageState,
        val exercises: List<ExerciseState>,
        val startAt: LocalDateTime
    ) : TrainingRecordingDirection

    data class ToExercise(
        val exercise: ExerciseState,
        val resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>
    ) :
        TrainingRecordingDirection

    data object Back :
        TrainingRecordingDirection
}
