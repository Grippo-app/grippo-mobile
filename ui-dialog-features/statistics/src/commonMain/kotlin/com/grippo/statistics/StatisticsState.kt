package com.grippo.statistics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.metrics.volume.VolumeSeriesState

@Immutable
public data class StatisticsState(
    val mode: StatisticsMode,
    val total: TrainingTotalState? = null,
    val exerciseVolume: VolumeSeriesState? = null,
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
