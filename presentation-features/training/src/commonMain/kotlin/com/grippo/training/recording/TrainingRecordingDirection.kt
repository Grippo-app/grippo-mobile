package com.grippo.training.recording

import com.grippo.core.models.BaseDirection

internal sealed interface TrainingRecordingDirection : BaseDirection {
    data object ToSuccess : TrainingRecordingDirection
    data class ToExercise(val id: String?) : TrainingRecordingDirection
    data object Back : TrainingRecordingDirection
}