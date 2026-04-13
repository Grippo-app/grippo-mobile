package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.DensityFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceTrendStatusState
import com.grippo.data.features.api.metrics.models.PerformanceMetric
import com.grippo.data.features.api.metrics.models.PerformanceTrendStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<PerformanceMetric>.toState(): ImmutableList<PerformanceMetricState> {
    return map { it.toState() }.toPersistentList()
}

private fun PerformanceMetric.toState(): PerformanceMetricState {
    return when (this) {
        is PerformanceMetric.DurationMetric -> PerformanceMetricState.Duration(
            deltaPercentage = deltaPercentage,
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = DurationFormatState.of(current),
            average = DurationFormatState.of(average),
            best = DurationFormatState.of(best),
            status = status.toState(),
        )

        is PerformanceMetric.IntensityMetric -> PerformanceMetricState.Intensity(
            deltaPercentage = deltaPercentage,
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = IntensityFormatState.of(current),
            average = IntensityFormatState.of(average),
            best = IntensityFormatState.of(best),
            status = status.toState(),
        )

        is PerformanceMetric.RepetitionsMetric -> PerformanceMetricState.Repetitions(
            deltaPercentage = deltaPercentage,
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = RepetitionsFormatState.of(current),
            average = RepetitionsFormatState.of(average),
            best = RepetitionsFormatState.of(best),
            status = status.toState(),
        )

        is PerformanceMetric.VolumeMetric -> PerformanceMetricState.Volume(
            deltaPercentage = deltaPercentage,
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = VolumeFormatState.of(current),
            average = VolumeFormatState.of(average),
            best = VolumeFormatState.of(best),
            status = status.toState(),
        )

        is PerformanceMetric.DensityMetric -> PerformanceMetricState.Density(
            deltaPercentage = deltaPercentage,
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = DensityFormatState.of(current),
            average = DensityFormatState.of(average),
            best = DensityFormatState.of(best),
            status = status.toState(),
        )
    }
}

private fun PerformanceTrendStatus.toState(): PerformanceTrendStatusState {
    return when (this) {
        PerformanceTrendStatus.Record -> PerformanceTrendStatusState.Record
        PerformanceTrendStatus.Improved -> PerformanceTrendStatusState.Improved
        PerformanceTrendStatus.Stable -> PerformanceTrendStatusState.Stable
        PerformanceTrendStatus.Declined -> PerformanceTrendStatusState.Declined
    }
}
