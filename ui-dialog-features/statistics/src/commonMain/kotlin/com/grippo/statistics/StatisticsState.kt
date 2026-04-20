package com.grippo.statistics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.TrainingTotalState
import com.grippo.core.state.metrics.VolumeSeriesState

@Immutable
public data class StatisticsState(
    val mode: StatisticsMode,

    // === Basic metrics chips ===
    val total: TrainingTotalState? = null,

    // === Exercise volume (bar) ===
    val exerciseVolume: VolumeSeriesState? = null,

    // === Muscle analysis (progress/heatmap) ===
    val muscleLoad: MuscleLoadSummaryState? = null,
)

@Immutable
public sealed interface StatisticsMode {
    @Immutable
    public data class Trainings(
        val range: DateRangeFormatState
    ) : StatisticsMode

    @Immutable
    public data object Exercises : StatisticsMode
}
