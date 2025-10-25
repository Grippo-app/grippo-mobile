package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.toolkit.calculation.models.MetricSeries
import com.grippo.toolkit.calculation.models.MuscleLoadSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExerciseExampleState(
    val example: ExerciseExampleState? = null,
    val muscleLoad: MuscleLoadSummary? = null,
    val recent: ImmutableList<ExerciseState> = persistentListOf(),
    val achievements: ImmutableList<ExerciseState> = persistentListOf(),

    // === Exercise volume (bar) ===
    val exerciseVolume: MetricSeries? = null,
)
