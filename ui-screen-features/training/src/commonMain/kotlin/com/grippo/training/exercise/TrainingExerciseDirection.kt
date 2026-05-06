package com.grippo.training.exercise

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.trainings.ExerciseState

internal sealed interface TrainingExerciseDirection : BaseDirection {
    data object Back : TrainingExerciseDirection
    data class Update(val exercise: ExerciseState) : TrainingExerciseDirection
    data class Save(val exercise: ExerciseState) : TrainingExerciseDirection
}
