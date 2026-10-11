package com.grippo.training.exercise

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.screen.api.ExercisesRouter
import com.grippo.core.state.trainings.ExerciseState

internal sealed interface TrainingExerciseDirection : BaseDirection {
    data class ToLibrary(val route: ExercisesRouter.Library) : TrainingExerciseDirection
    data object Back : TrainingExerciseDirection
    data class Update(val exercise: ExerciseState) : TrainingExerciseDirection
    data class Delete(val id: String) : TrainingExerciseDirection
    data class Save(val exercise: ExerciseState) : TrainingExerciseDirection
}
