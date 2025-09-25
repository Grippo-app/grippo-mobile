package com.grippo.home.statistics

import androidx.compose.runtime.Immutable
import com.grippo.calculation.models.MuscleImages
import com.grippo.calculation.models.MuscleLoadBreakdown
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
    val totalVolume: VolumeFormatState = VolumeFormatState.of(0f),
    val totalRepetitions: RepetitionsFormatState = RepetitionsFormatState.of(0),
    val averageIntensity: IntensityFormatState = IntensityFormatState.of(0f),

    // === Exercise volume (bar) ===
    val exerciseVolumeData: DSBarData = DSBarData(items = emptyList()),

    // === Muscle analysis (progress/heatmap) ===
    val muscleLoadData: DSProgressData = DSProgressData(items = emptyList()),
    val muscleLoadMuscles: MuscleLoadBreakdown = MuscleLoadBreakdown(entries = emptyList()),
    val muscleLoadImages: MuscleImages? = null,

    // === Exercise example distributions (pie) ===
    val categoryDistributionData: DSPieData = DSPieData(slices = emptyList()),
    val weightTypeDistributionData: DSPieData = DSPieData(slices = emptyList()),
    val forceTypeDistributionData: DSPieData = DSPieData(slices = emptyList()),

    // === Temporal heatmap ===
    val temporalHeatmapData: DSHeatmapData = DSHeatmapData(
        rows = 0,
        cols = 0,
        values01 = emptyList()
    )
)
