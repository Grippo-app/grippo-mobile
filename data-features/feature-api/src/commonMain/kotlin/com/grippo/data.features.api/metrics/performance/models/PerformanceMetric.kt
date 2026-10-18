package com.grippo.data.features.api.metrics.performance.models

import kotlin.time.Duration

public sealed interface PerformanceMetric {
    public val type: PerformanceMetricType
    public val currentVsAveragePercentage: Double?
    public val status: PerformanceTrendStatus

    public data class DurationMetric(
        override val currentVsAveragePercentage: Double?,
        val current: Duration?,
        val average: Duration?,
        val best: Duration?,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Duration
    }

    public data class VolumeMetric(
        override val currentVsAveragePercentage: Double?,
        val current: Float?,
        val average: Float?,
        val best: Float?,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Volume
    }

    /** Training volume per minute; it describes workout density, not exercise quality. */
    public data class DensityMetric(
        override val currentVsAveragePercentage: Double?,
        val current: Float?,
        val average: Float?,
        val best: Float?,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Density
    }

    public data class RepetitionsMetric(
        override val currentVsAveragePercentage: Double?,
        val current: Int?,
        val average: Double?,
        val best: Int?,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Repetitions
    }

    public data class IntensityMetric(
        override val currentVsAveragePercentage: Double?,
        val current: Float?,
        val average: Float?,
        val best: Float?,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Intensity
    }
}
