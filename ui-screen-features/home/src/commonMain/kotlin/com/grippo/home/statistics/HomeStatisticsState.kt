package com.grippo.home.statistics

import androidx.compose.runtime.Immutable
import com.grippo.calculation.models.MuscleLoadSummary
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class HomeStatisticsState(
    val period: PeriodState = PeriodState.ThisWeek,
    val trainings: ImmutableList<TrainingState> = persistentListOf(),
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
    val forceTypeDistribution: DSPieData? = null,

    // === Temporal heatmap ===
    val temporalHeatmap: DSHeatmapData? = null,
)
