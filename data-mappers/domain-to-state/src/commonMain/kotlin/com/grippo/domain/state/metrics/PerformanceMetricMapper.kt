package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceTrendStatusState
import com.grippo.data.features.api.metrics.models.PerformanceMetric
import com.grippo.data.features.api.metrics.models.PerformanceTrendStatus

public fun List<PerformanceMetric>.toState(): List<PerformanceMetricState> {
    return map { it.toState() }
}

private fun PerformanceMetric.toState(): PerformanceMetricState {
    return when (this) {
        is PerformanceMetric.DurationMetric -> PerformanceMetricState.Duration(
            deltaPercentage = deltaPercentage,
            current = current,
            average = average,
            best = best,
            status = status.toState(),
        )

        is PerformanceMetric.IntensityMetric -> PerformanceMetricState.Intensity(
            deltaPercentage = deltaPercentage,
            current = IntensityFormatState.of(current),
            average = IntensityFormatState.of(average),
            best = IntensityFormatState.of(best),
            status = status.toState(),
        )

        is PerformanceMetric.RepetitionsMetric -> PerformanceMetricState.Repetitions(
            deltaPercentage = deltaPercentage,
            current = RepetitionsFormatState.of(current),
            average = RepetitionsFormatState.of(average),
            best = RepetitionsFormatState.of(best),
            status = status.toState(),
        )

        is PerformanceMetric.VolumeMetric -> PerformanceMetricState.Volume(
            deltaPercentage = deltaPercentage,
            current = VolumeFormatState.of(current),
            average = VolumeFormatState.of(average),
            best = VolumeFormatState.of(best),
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
