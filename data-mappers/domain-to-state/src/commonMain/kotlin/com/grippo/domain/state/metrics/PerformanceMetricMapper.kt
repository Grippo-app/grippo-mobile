package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.PerformanceMetricState as StatePerformanceMetric
import com.grippo.core.state.metrics.PerformanceTrendStatusState as StatePerformanceTrendStatus
import com.grippo.data.features.api.metrics.models.PerformanceMetric as DomainPerformanceMetric
import com.grippo.data.features.api.metrics.models.PerformanceTrendStatus as DomainPerformanceTrendStatus

public fun List<DomainPerformanceMetric>.toState(): List<StatePerformanceMetric> {
    return map { it.toState() }
}

private fun DomainPerformanceMetric.toState(): StatePerformanceMetric {
    return when (this) {
        is DomainPerformanceMetric.DurationMetric -> StatePerformanceMetric.Duration(
            deltaPercentage = deltaPercentage,
            current = current,
            average = average,
            best = best,
            status = status.toState(),
        )

        is DomainPerformanceMetric.IntensityMetric -> StatePerformanceMetric.Intensity(
            deltaPercentage = deltaPercentage,
            current = IntensityFormatState.of(current),
            average = IntensityFormatState.of(average),
            best = IntensityFormatState.of(best),
            status = status.toState(),
        )

        is DomainPerformanceMetric.RepetitionsMetric -> StatePerformanceMetric.Repetitions(
            deltaPercentage = deltaPercentage,
            current = RepetitionsFormatState.of(current),
            average = RepetitionsFormatState.of(average),
            best = RepetitionsFormatState.of(best),
            status = status.toState(),
        )

        is DomainPerformanceMetric.VolumeMetric -> StatePerformanceMetric.Volume(
            deltaPercentage = deltaPercentage,
            current = VolumeFormatState.of(current),
            average = VolumeFormatState.of(average),
            best = VolumeFormatState.of(best),
            status = status.toState(),
        )
    }
}

private fun DomainPerformanceTrendStatus.toState(): StatePerformanceTrendStatus {
    return when (this) {
        DomainPerformanceTrendStatus.Record -> StatePerformanceTrendStatus.Record
        DomainPerformanceTrendStatus.Improved -> StatePerformanceTrendStatus.Improved
        DomainPerformanceTrendStatus.Stable -> StatePerformanceTrendStatus.Stable
        DomainPerformanceTrendStatus.Declined -> StatePerformanceTrendStatus.Declined
    }
}
