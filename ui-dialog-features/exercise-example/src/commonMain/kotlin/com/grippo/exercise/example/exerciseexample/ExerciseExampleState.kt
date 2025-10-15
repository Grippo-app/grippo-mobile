package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.calculation.models.MuscleLoadSummary
import com.grippo.core.state.examples.ExerciseExampleState

@Immutable
public data class ExerciseExampleState(
    val example: ExerciseExampleState? = null,
    val muscleLoad: MuscleLoadSummary? = null,
)
