package com.grippo.training.recording

import com.grippo.core.models.BaseDirection
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

internal sealed interface TrainingRecordingDirection : BaseDirection {
    data class ToCompleted(val training: TrainingState) : TrainingRecordingDirection
    data class ToExercise(val exercise: ExerciseState) : TrainingRecordingDirection
    data object Back : TrainingRecordingDirection
}