package com.grippo.domain.state.metrics.performance

import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.data.features.api.metrics.performance.models.PerformanceMetric
import com.grippo.data.features.api.metrics.performance.models.PerformanceTrendStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<PerformanceMetric>.toState(): ImmutableList<PerformanceMetricState> {
    return map { it.toState() }.toPersistentList()
}

private fun PerformanceMetric.toState(): PerformanceMetricState {
    return when (this) {
        is PerformanceMetric.DurationMetric -> PerformanceMetricState.Duration(
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = current,
            average = average,
            best = best,
            status = status.toState(),
        )

        is PerformanceMetric.IntensityMetric -> PerformanceMetricState.Intensity(
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = current,
            average = average,
            best = best,
            status = status.toState(),
        )

        is PerformanceMetric.RepetitionsMetric -> PerformanceMetricState.Repetitions(
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = current,
            average = average,
            best = best,
            status = status.toState(),
        )

        is PerformanceMetric.VolumeMetric -> PerformanceMetricState.Volume(
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = current,
            average = average,
            best = best,
            status = status.toState(),
        )

        is PerformanceMetric.DensityMetric -> PerformanceMetricState.Density(
            currentVsAveragePercentage = currentVsAveragePercentage,
            current = current,
            average = average,
            best = best,
            status = status.toState(),
        )
    }
}

private fun PerformanceTrendStatus.toState(): PerformanceTrendStatusState {
    return when (this) {
        PerformanceTrendStatus.Empty -> PerformanceTrendStatusState.Empty
        PerformanceTrendStatus.Record -> PerformanceTrendStatusState.Record
        PerformanceTrendStatus.Improved -> PerformanceTrendStatusState.Improved
        PerformanceTrendStatus.Stable -> PerformanceTrendStatusState.Stable
        PerformanceTrendStatus.Declined -> PerformanceTrendStatusState.Declined
    }
}
