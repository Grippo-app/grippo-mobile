package com.grippo.training.recording

import com.grippo.core.models.BaseDirection
import com.grippo.state.trainings.ExerciseState

internal sealed interface TrainingRecordingDirection : BaseDirection {
    data object ToSuccess : TrainingRecordingDirection
    data class ToExercise(val exercise: ExerciseState) : TrainingRecordingDirection
    data object Back : TrainingRecordingDirection
}