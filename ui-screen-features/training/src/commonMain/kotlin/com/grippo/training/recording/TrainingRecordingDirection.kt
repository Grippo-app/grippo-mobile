package com.grippo.training.recording

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

internal sealed interface TrainingRecordingDirection : BaseDirection {
    data class ToCompleted(
        val stage: StageState,
        val exercises: List<ExerciseState>,
        val startAt: LocalDateTime
    ) : TrainingRecordingDirection

    data class ToExercise(val exercise: ExerciseState) :
        TrainingRecordingDirection

    data object Back :
        TrainingRecordingDirection
}