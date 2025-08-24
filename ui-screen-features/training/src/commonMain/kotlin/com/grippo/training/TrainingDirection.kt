package com.grippo.training

import com.grippo.core.models.BaseDirection
import com.grippo.state.trainings.ExerciseState

public sealed interface TrainingDirection : BaseDirection {
    public data object Close : TrainingDirection
    public data object Back : TrainingDirection
    public data object ToRecording : TrainingDirection
    public data class ToExercise(val exercise: ExerciseState) : TrainingDirection
    public data object ToSuccess : TrainingDirection
}