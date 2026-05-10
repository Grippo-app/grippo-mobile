package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.core.state.achievements.AchievementState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxState
import com.grippo.core.state.metrics.volume.VolumeSeriesState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.dialog.api.DialogConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExerciseExampleState(
    val mode: ExerciseExampleModeState = ExerciseExampleModeState.Default,
    val example: ExerciseExampleState? = null,
    val muscleLoad: MuscleLoadSummaryState? = null,
    val recent: ImmutableList<ExerciseState> = persistentListOf(),
    val achievements: ImmutableList<AchievementState> = persistentListOf(),
    val estimatedOneRepMax: EstimatedOneRepMaxState? = null,

    // === Exercise volume (bar) ===
    val exerciseVolume: VolumeSeriesState? = null,
)

@Immutable
public sealed interface ExerciseExampleModeState {

    @Immutable
    public data object Default : ExerciseExampleModeState

    @Immutable
    public data class Action(val title: String) : ExerciseExampleModeState
}

internal fun DialogConfig.ExerciseExample.Mode.toState(): ExerciseExampleModeState = when (this) {
    DialogConfig.ExerciseExample.Mode.Default -> ExerciseExampleModeState.Default
    is DialogConfig.ExerciseExample.Mode.Action -> ExerciseExampleModeState.Action(title = title)
}
