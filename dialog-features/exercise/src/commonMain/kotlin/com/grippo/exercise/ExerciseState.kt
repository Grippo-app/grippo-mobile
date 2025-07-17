package com.grippo.exercise

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.trainings.models.ExerciseState as TrainingExerciseState

@Immutable
public data class ExerciseState(
    val exercise: TrainingExerciseState? = null,
)