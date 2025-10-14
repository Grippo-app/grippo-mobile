package com.grippo.training.exercise

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.state.trainings.ExerciseState

internal sealed interface ExerciseDirection : BaseDirection {
    data object Back : ExerciseDirection
    data class Save(val exercise: ExerciseState) : ExerciseDirection
}
