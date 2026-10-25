package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.DensityFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.AppColor
import kotlin.math.roundToInt
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
public sealed interface PerformanceMetricState {
    public val type: PerformanceMetricTypeState
    public val currentVsAveragePercentage: Double?
    public val status: PerformanceTrendStatusState

    public fun isEmpty(): Boolean = status == PerformanceTrendStatusState.Empty

    public fun hasAverage(): Boolean = when (this) {
        is Duration -> average != null
        is Volume -> average != null
        is Density -> average != null
        is Repetitions -> average != null
        is Intensity -> average != null
    }

    public fun hasComparison(): Boolean =
        currentVsAveragePercentage != null || status == PerformanceTrendStatusState.Record

    public fun votes(): Boolean = !isEmpty() && this !is Duration

    public fun showsComparison(): Boolean = votes() && hasComparison()

    public fun chartValue(): Float? {
        if (isEmpty()) {
            return null
        }

        val value = when (this) {
            is Duration -> current?.toDouble(DurationUnit.MINUTES)?.toFloat()
            is Volume -> current
            is Density -> current
            is Repetitions -> current?.toFloat()
            is Intensity -> current
        }
        return value?.takeIf { it.isFinite() && it >= 0f }
    }

    public fun averageChartValue(): Float? {
        val value = when (this) {
            is Duration -> average?.toDouble(DurationUnit.MINUTES)?.toFloat()
            is Volume -> average
            is Density -> average
            is Repetitions -> average?.toFloat()
            is Intensity -> average
        }
        return value?.takeIf { it.isFinite() && it >= 0f }
    }

    @Composable
    public fun displayValue(value: Float): String = when (this) {
        is Duration -> DurationFormatState.shortMinutes(value.toDouble().minutes)
        is Volume -> VolumeFormatState.shortTotal(value)
        is Density -> DensityFormatState.shortTotal(value)
        is Repetitions -> RepetitionsFormatState.shortAverage(value.toDouble())
        is Intensity -> IntensityFormatState.shortAverage(value)
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
    public fun averageDeltaColor(): Color = when (band()) {
        Band.Gain -> AppTokens.colors.semantic.success
        Band.Loss -> AppTokens.colors.semantic.decline
        Band.Neutral -> AppTokens.colors.text.secondary
    }

    @Composable
    public fun indicatorColors(): AppColor.Charts.IndicatorColors.IndicatorColors = when (band()) {
        Band.Gain -> AppTokens.colors.charts.indicator.success
        Band.Loss -> AppTokens.colors.charts.indicator.decline
        Band.Neutral -> AppTokens.colors.charts.indicator.muted
    }

    private fun band(): Band {
        if (!votes()) {
            return Band.Neutral
        }

        return when (status) {
            PerformanceTrendStatusState.Record,
            PerformanceTrendStatusState.Improved -> Band.Gain

            PerformanceTrendStatusState.Declined -> Band.Loss

            PerformanceTrendStatusState.Stable,
            PerformanceTrendStatusState.Empty -> Band.Neutral
        }
    }

    private enum class Band {
        Gain,
        Loss,
        Neutral,
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

public fun stubPerformanceMetricsByType(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetrics().distinctBy { it.type }.toPersistentList()

public fun stubPerformanceMetricsWithoutBaseline(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetricsByType().map { metric ->
        when (metric) {
            is PerformanceMetricState.Duration -> metric.copy(
                currentVsAveragePercentage = null,
                average = null,
                best = metric.current,
                status = PerformanceTrendStatusState.Stable,
            )

            is PerformanceMetricState.Volume -> metric.copy(
                currentVsAveragePercentage = null,
                average = null,
                best = metric.current,
                status = PerformanceTrendStatusState.Stable,
            )

            is PerformanceMetricState.Density -> metric.copy(
                currentVsAveragePercentage = null,
                average = null,
                best = metric.current,
                status = PerformanceTrendStatusState.Stable,
            )

            is PerformanceMetricState.Repetitions -> metric.copy(
                currentVsAveragePercentage = null,
                average = null,
                best = metric.current,
                status = PerformanceTrendStatusState.Stable,
            )

            is PerformanceMetricState.Intensity -> metric.copy(
                currentVsAveragePercentage = null,
                average = null,
                best = metric.current,
                status = PerformanceTrendStatusState.Stable,
            )
        }
    }.toPersistentList()

public fun stubPerformanceMetricsImproved(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetricsByType().map { metric ->
        when (metric) {
            is PerformanceMetricState.Duration -> metric

            is PerformanceMetricState.Volume -> metric.copy(
                best = 1_250f,
                status = PerformanceTrendStatusState.Improved,
            )

            is PerformanceMetricState.Density -> metric.copy(status = PerformanceTrendStatusState.Improved)

            is PerformanceMetricState.Repetitions -> metric.copy(status = PerformanceTrendStatusState.Improved)

            is PerformanceMetricState.Intensity -> metric.copy(
                currentVsAveragePercentage = 8.6,
                current = 38f,
                status = PerformanceTrendStatusState.Improved,
            )
        }
    }.toPersistentList()

private const val PREVIEW_DECLINE_PERCENTAGE = -12.0
private const val PREVIEW_DECLINE_RATIO = 1 + PREVIEW_DECLINE_PERCENTAGE / 100

public fun stubPerformanceMetricsDeclined(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetricsByType().map { metric ->
        when (metric) {
            is PerformanceMetricState.Duration -> metric

            is PerformanceMetricState.Volume -> metric.copy(
                currentVsAveragePercentage = PREVIEW_DECLINE_PERCENTAGE,
                current = metric.average?.times(PREVIEW_DECLINE_RATIO.toFloat()),
                status = PerformanceTrendStatusState.Declined,
            )

            is PerformanceMetricState.Density -> metric.copy(
                currentVsAveragePercentage = PREVIEW_DECLINE_PERCENTAGE,
                current = metric.average?.times(PREVIEW_DECLINE_RATIO.toFloat()),
                status = PerformanceTrendStatusState.Declined,
            )

            is PerformanceMetricState.Repetitions -> metric.copy(
                currentVsAveragePercentage = PREVIEW_DECLINE_PERCENTAGE,
                current = metric.average?.times(PREVIEW_DECLINE_RATIO)?.roundToInt(),
                status = PerformanceTrendStatusState.Declined,
            )

            is PerformanceMetricState.Intensity -> metric.copy(
                currentVsAveragePercentage = PREVIEW_DECLINE_PERCENTAGE,
                current = metric.average?.times(PREVIEW_DECLINE_RATIO.toFloat()),
                status = PerformanceTrendStatusState.Declined,
            )
        }
    }.toPersistentList()

public fun stubEmptyPerformanceMetrics(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetricsByType().map { metric ->
        when (metric) {
            is PerformanceMetricState.Duration -> metric.copy(
                currentVsAveragePercentage = null,
                current = null,
                average = null,
                best = null,
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Volume -> metric.copy(
                currentVsAveragePercentage = null,
                current = null,
                average = null,
                best = null,
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Density -> metric.copy(
                currentVsAveragePercentage = null,
                current = null,
                average = null,
                best = null,
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Repetitions -> metric.copy(
                currentVsAveragePercentage = null,
                current = null,
                average = null,
                best = null,
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Intensity -> metric.copy(
                currentVsAveragePercentage = null,
                current = null,
                average = null,
                best = null,
                status = PerformanceTrendStatusState.Empty,
            )
        }
    }.toPersistentList()
