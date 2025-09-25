package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.calculation.models.MuscleLoadBreakdown
import com.grippo.design.components.chart.DSProgressData
import com.grippo.state.exercise.examples.ExerciseExampleState

@Immutable
public data class ExerciseExampleState(
    val example: ExerciseExampleState? = null,
    val muscleLoadData: DSProgressData = DSProgressData(items = emptyList()),
    val muscleLoadMuscles: MuscleLoadBreakdown = MuscleLoadBreakdown(entries = emptyList()),
)
