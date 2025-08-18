package com.grippo.training

import com.grippo.core.models.BaseDirection

public sealed interface TrainingDirection : BaseDirection {
    public data object Back : TrainingDirection
    public data object ToRecording : TrainingDirection
    public data class ToExercise(val id: String?) : TrainingDirection
    public data object ToSuccess : TrainingDirection
}