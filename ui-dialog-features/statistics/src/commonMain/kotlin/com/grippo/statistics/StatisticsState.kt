package com.grippo.statistics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.metrics.ExerciseDistributionState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.core.state.metrics.TrainingTotalState
import com.grippo.core.state.metrics.VolumeSeriesState
import com.grippo.toolkit.date.utils.DateRange

@Immutable
public data class StatisticsState(
    val mode: StatisticsMode,

    // === Basic metrics chips ===
    val total: TrainingTotalState? = null,

    // === Exercise volume (bar) ===
    val exerciseVolume: VolumeSeriesState? = null,

    // === Muscle analysis (progress/heatmap) ===
    val muscleLoad: MuscleLoadSummaryState? = null,

    // === Exercise example distributions (pie) ===
    val categoryDistribution: ExerciseDistributionState<CategoryEnumState>? = null,
    val weightTypeDistribution: ExerciseDistributionState<WeightTypeEnumState>? = null,
    val forceTypeDistribution: ExerciseDistributionState<ForceTypeEnumState>? = null,

    // === Training profile (radar) ===
    val profile: TrainingLoadProfileState? = null,
)

@Immutable
public sealed interface StatisticsMode {
    @Immutable
    public data class Trainings(
        val range: DateRange
    ) : StatisticsMode

    @Immutable
    public data object Exercises : StatisticsMode
}
