package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.calculation.models.MuscleLoadSummary
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,

    // === Main data ===
    val exercises: ImmutableList<ExerciseState> = persistentListOf(),
    val startAt: LocalDateTime = DateTimeUtils.now(),

    // === Filters / Sorting ===
    val examples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    val muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),

    // === Basic metrics chips ===
    val totalVolume: VolumeFormatState? = null,
    val totalRepetitions: RepetitionsFormatState? = null,
    val averageIntensity: IntensityFormatState? = null,

    // === Exercise volume (bar) ===
    val exerciseVolume: DSBarData? = null,

    // === Muscle analysis (progress/heatmap) ===
    val muscleLoad: DSProgressData? = null,
    val muscleLoadSummary: MuscleLoadSummary? = null,

    // === Exercise example distributions (pie) ===
    val categoryDistribution: DSPieData? = null,
    val weightTypeDistribution: DSPieData? = null,
    val forceTypeDistribution: DSPieData? = null
)

@Immutable
internal enum class RecordingTab {
    Exercises,
    Stats
}
