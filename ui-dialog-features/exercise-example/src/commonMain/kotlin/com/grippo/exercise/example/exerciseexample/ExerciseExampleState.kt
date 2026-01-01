package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.core.state.achevements.AchievementState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.metrics.MuscleLoadSummary
import com.grippo.core.state.metrics.VolumeSeriesState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExerciseExampleState(
    val example: ExerciseExampleState? = null,
    val muscleLoad: MuscleLoadSummary? = null,
    val recent: ImmutableList<ExerciseState> = persistentListOf(),
    val achievements: ImmutableList<AchievementState> = persistentListOf(),

    // === Exercise volume (bar) ===
    val exerciseVolume: VolumeSeriesState? = null,
)
