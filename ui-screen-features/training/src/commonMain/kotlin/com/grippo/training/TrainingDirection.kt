package com.grippo.training

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.state.stage.StageState
import com.grippo.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

public sealed interface TrainingDirection : BaseDirection {
    public data object Close : TrainingDirection

    public data object Back : TrainingDirection

    public data class ToRecording(
        val stage: StageState
    ) : TrainingDirection

    public data class ToExercise(
        val exercise: ExerciseState
    ) : TrainingDirection

    public data class ToCompleted(
        val stage: StageState,
        val exercises: List<ExerciseState>,
        val startAt: LocalDateTime,
    ) : TrainingDirection
}