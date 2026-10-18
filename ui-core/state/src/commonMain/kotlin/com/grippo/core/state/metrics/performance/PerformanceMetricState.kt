package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DensityFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
public sealed interface PerformanceMetricState {
    public val type: PerformanceMetricTypeState
    public val currentVsAveragePercentage: Double?
    public val status: PerformanceTrendStatusState

    public fun isEmpty(): Boolean = status == PerformanceTrendStatusState.Empty

    /** Values retain their calculation precision; only display text is rounded. */
    public fun chartValue(): Float? {
        if (isEmpty()) return null
        val value = when (this) {
            is Duration -> current?.toDouble(DurationUnit.MINUTES)?.toFloat()
            is Volume -> current
            is Density -> current
            is Repetitions -> current?.toFloat()
            is Intensity -> current
        }
        return value?.takeIf { it.isFinite() && it > 0f }
    }

    @Composable
    public fun currentDisplay(): String = when (this) {
        is Duration -> DurationFormatState.shortMinutes(current)
        is Volume -> VolumeFormatState.shortTotal(current)
        is Density -> DensityFormatState.shortTotal(current)
        is Repetitions -> RepetitionsFormatState.shortAverage(current?.toDouble())
        is Intensity -> IntensityFormatState.shortAverage(current)
    }

    @Composable
    public fun averageDisplay(): String = when (this) {
        is Duration -> DurationFormatState.shortMinutes(average)
        is Volume -> VolumeFormatState.shortTotal(average)
        is Density -> DensityFormatState.shortTotal(average)
        is Repetitions -> RepetitionsFormatState.shortAverage(average)
        is Intensity -> IntensityFormatState.shortAverage(average)
    }

    @Composable
    public fun bestDisplay(): String = when (this) {
        is Duration -> DurationFormatState.shortMinutes(best)
        is Volume -> VolumeFormatState.shortTotal(best)
        is Density -> DensityFormatState.shortTotal(best)
        is Repetitions -> RepetitionsFormatState.shortAverage(best?.toDouble())
        is Intensity -> IntensityFormatState.shortAverage(best)
    }

    @Immutable
    public data class Volume(
        override val currentVsAveragePercentage: Double?,
        val current: Float?,
        val average: Float?,
        val best: Float?,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Volume
    }

    @Immutable
    public data class Density(
        override val currentVsAveragePercentage: Double?,
        val current: Float?,
        val average: Float?,
        val best: Float?,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Density
    }

    @Immutable
    public data class Duration(
        override val currentVsAveragePercentage: Double?,
        val current: KotlinDuration?,
        val average: KotlinDuration?,
        val best: KotlinDuration?,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Duration
    }

    @Immutable
    public data class Repetitions(
        override val currentVsAveragePercentage: Double?,
        val current: Int?,
        val average: Double?,
        val best: Int?,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Repetitions
    }

    @Immutable
    public data class Intensity(
        override val currentVsAveragePercentage: Double?,
        val current: Float?,
        val average: Float?,
        val best: Float?,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Intensity
    }
}

public fun stubPerformanceTrendHistory(): ImmutableList<PerformanceTrendHistoryEntry> {
    val date = LocalDateTime(2026, 10, 15, 10, 0)
    val anyRange = DateRangePresets.infinity()
    return persistentListOf(
        PerformanceTrendHistoryEntry(
            trainingId = "preview-performance-1",
            endDate = DateTimeFormatState.of(
                value = date,
                range = anyRange,
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            metric = stubPerformanceMetrics().first()
        ),
        PerformanceTrendHistoryEntry(
            trainingId = "preview-performance-2",
            endDate = DateTimeFormatState.of(
                value = date,
                range = anyRange,
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            metric = stubPerformanceMetrics().first()
        ),
        PerformanceTrendHistoryEntry(
            trainingId = "preview-performance-3",
            endDate = DateTimeFormatState.of(
                value = date,
                range = anyRange,
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            metric = stubPerformanceMetrics().first()
        ),
    )
}

public fun stubPerformanceMetrics(): ImmutableList<PerformanceMetricState> {
    return persistentListOf(
        PerformanceMetricState.Volume(
            currentVsAveragePercentage = 33.0,
            current = 1_200f,
            average = 900f,
            best = 1_200f,
            status = PerformanceTrendStatusState.Record
        ),
        PerformanceMetricState.Density(
            currentVsAveragePercentage = 10.0,
            current = 22f,
            average = 20f,
            best = 24f,
            status = PerformanceTrendStatusState.Improved
        ),
        PerformanceMetricState.Volume(
            currentVsAveragePercentage = -12.0,
            current = 780f,
            average = 890f,
            best = 1_050f,
            status = PerformanceTrendStatusState.Declined
        ),
        PerformanceMetricState.Duration(
            currentVsAveragePercentage = -7.0,
            current = 65.minutes,
            average = 70.minutes,
            best = 80.minutes,
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Repetitions(
            currentVsAveragePercentage = 12.0,
            current = 84,
            average = 75.0,
            best = 92,
            status = PerformanceTrendStatusState.Improved
        ),
        PerformanceMetricState.Repetitions(
            currentVsAveragePercentage = 0.0,
            current = 70,
            average = 70.0,
            best = 85,
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Intensity(
            currentVsAveragePercentage = 3.0,
            current = 36f,
            average = 35f,
            best = 42f,
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Intensity(
            currentVsAveragePercentage = 18.0,
            current = 40f,
            average = 34f,
            best = 40f,
            status = PerformanceTrendStatusState.Record
        )
    )
}

public fun stubEmptyPerformanceMetric(): PerformanceMetricState =
    PerformanceMetricState.Volume(
        currentVsAveragePercentage = null,
        current = null,
        average = null,
        best = null,
        status = PerformanceTrendStatusState.Empty,
    )
