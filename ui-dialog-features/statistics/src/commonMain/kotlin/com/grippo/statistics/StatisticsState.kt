package com.grippo.statistics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.muscles.metrics.MuscleLoadSummary
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingMetrics
import com.grippo.core.state.trainings.TrainingState
import com.grippo.toolkit.calculation.models.DistributionBreakdown
import com.grippo.toolkit.calculation.models.MetricSeries
import com.grippo.toolkit.calculation.models.MuscleLoadMatrix
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class StatisticsState(
    val mode: StatisticsMode,

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

@Immutable
public sealed interface StatisticsMode {
    @Immutable
    public data class Trainings(
        val trainings: ImmutableList<TrainingState> = persistentListOf(),
        val range: DateRange
    ) : StatisticsMode

    @Immutable
    public data class Exercises(
        val exercises: ImmutableList<ExerciseState> = persistentListOf(),
    ) : StatisticsMode
}
