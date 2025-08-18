package com.grippo.training.exercise

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.stubExercise

@Immutable
internal data class ExerciseState(
    val exercise: ExerciseState = stubExercise(),
)
