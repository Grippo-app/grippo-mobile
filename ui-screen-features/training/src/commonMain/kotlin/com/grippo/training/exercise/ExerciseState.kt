package com.grippo.training.exercise

import androidx.compose.runtime.Immutable
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.trainings.ExerciseState

@Immutable
internal data class ExerciseState(
    val exercise: ExerciseState,
    val exerciseExample: ExerciseExampleState? = null,
)
