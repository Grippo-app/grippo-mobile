package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.muscles.models.MuscleState

@Immutable
public data class ExerciseExampleBundleState(
    val id: String,
    val muscle: MuscleState,
    val percentage: Int
)