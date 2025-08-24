package com.grippo.exercise

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.ExerciseState as TrainingExerciseState

@Immutable
public data class ExerciseState(
    val exercise: TrainingExerciseState? = null,
)