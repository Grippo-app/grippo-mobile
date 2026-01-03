package com.grippo.data.features.api.metrics.models

import kotlin.time.Duration

public sealed interface PerformanceMetric {
    public val type: PerformanceMetricType
    public val deltaPercentage: Int
    public val status: PerformanceTrendStatus

    public data class DurationMetric(
        override val deltaPercentage: Int,
        val current: Duration,
        val average: Duration,
        val best: Duration,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Duration
    }

    public data class VolumeMetric(
        override val deltaPercentage: Int,
        val current: Float,
        val average: Float,
        val best: Float,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Volume
    }

    /**
     * Volume per minute.
     *
     * Rationale:
     * - Duration itself is neutral (longer != better)
     * - Density is a better “efficiency / effort” proxy for highlights.
     */
    public data class DensityMetric(
        override val deltaPercentage: Int,
        val current: Float,
        val average: Float,
        val best: Float,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Density
    }

    public data class RepetitionsMetric(
        override val deltaPercentage: Int,
        val current: Int,
        val average: Int,
        val best: Int,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Repetitions
    }

    public data class IntensityMetric(
        override val deltaPercentage: Int,
        val current: Float,
        val average: Float,
        val best: Float,
        override val status: PerformanceTrendStatus,
    ) : PerformanceMetric {
        override val type: PerformanceMetricType = PerformanceMetricType.Intensity
    }
}

public enum class PerformanceTrendStatus {
    Record,
    Improved,
    Stable,
    Declined
}

public enum class PerformanceMetricType {
    Duration,
    Volume,
    Density,
    Repetitions,
    Intensity,
}
