package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.core.state.achievements.AchievementState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxState
import com.grippo.core.state.metrics.volume.VolumeSeriesState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.dialog.api.DialogConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExerciseExampleDetailsState(
    val mode: ExerciseExampleDetailsModeState = ExerciseExampleDetailsModeState.Default,
    val example: ExerciseExampleState? = null,
    val muscleLoad: MuscleLoadSummaryState? = null,
    val recent: ImmutableList<ExerciseState> = persistentListOf(),
    val achievements: ImmutableList<AchievementState> = persistentListOf(),
    val estimatedOneRepMax: EstimatedOneRepMaxState? = null,
    val exerciseVolume: VolumeSeriesState? = null,
)

@Immutable
public sealed interface ExerciseExampleDetailsModeState {

    @Immutable
    public data object Default : ExerciseExampleDetailsModeState

    @Immutable
    public data class Action(val title: UiText) : ExerciseExampleDetailsModeState

    public companion object {
        internal fun of(value: DialogConfig.ExerciseExampleDetails.Mode): ExerciseExampleDetailsModeState = when (value) {
            DialogConfig.ExerciseExampleDetails.Mode.Default -> Default
            is DialogConfig.ExerciseExampleDetails.Mode.Action -> Action(title = value.title)
        }
    }
}
