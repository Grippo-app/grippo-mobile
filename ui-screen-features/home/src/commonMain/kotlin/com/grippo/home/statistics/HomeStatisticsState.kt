package com.grippo.home.statistics

import androidx.compose.runtime.Immutable
import com.grippo.calculation.models.DistributionBreakdown
import com.grippo.calculation.models.MetricSeries
import com.grippo.calculation.models.MuscleLoadMatrix
import com.grippo.calculation.models.MuscleLoadSummary
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.TrainingMetrics
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
    val totalMetrics: TrainingMetrics? = null,

    // === Exercise volume (bar) ===
    val exerciseVolume: MetricSeries? = null,

    // === Muscle analysis (progress/heatmap) ===
    val muscleLoad: MuscleLoadSummary? = null,

    // === Exercise example distributions (pie) ===
    val categoryDistribution: DistributionBreakdown? = null,
    val weightTypeDistribution: DistributionBreakdown? = null,
    val forceTypeDistribution: DistributionBreakdown? = null,

    // === Temporal heatmap ===
    val temporalHeatmap: MuscleLoadMatrix? = null,
)
