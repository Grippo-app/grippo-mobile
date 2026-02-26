package com.grippo.training.exercise

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.trainings.ExerciseState

internal sealed interface ExerciseDirection : BaseDirection {
    data object Back : ExerciseDirection
    data class Update(val exercise: ExerciseState) : ExerciseDirection
    data class Save(val exercise: ExerciseState) : ExerciseDirection
}
