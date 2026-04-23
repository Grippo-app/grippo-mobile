package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DensityFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Intensity
import com.grippo.design.resources.provider.icons.Repeat
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.icons.Volume
import com.grippo.design.resources.provider.performance_metric_density
import com.grippo.design.resources.provider.performance_metric_intensity
import com.grippo.design.resources.provider.performance_trend_desc_density
import com.grippo.design.resources.provider.performance_trend_desc_duration
import com.grippo.design.resources.provider.performance_trend_desc_intensity
import com.grippo.design.resources.provider.performance_trend_desc_repetitions
import com.grippo.design.resources.provider.performance_trend_desc_volume
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
@Immutable
public enum class PerformanceMetricTypeState {
    Duration,
    Volume,
    Density,
    Repetitions,
    Intensity;

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            Duration -> AppTokens.icons.Timer
            Volume -> AppTokens.icons.Volume
            Density -> AppTokens.icons.Timer
            Repetitions -> AppTokens.icons.Repeat
            Intensity -> AppTokens.icons.Intensity
        }
    }

    @Composable
    public fun description(): String {
        return when (this) {
            Duration -> AppTokens.strings.res(Res.string.performance_trend_desc_duration)
            Volume -> AppTokens.strings.res(Res.string.performance_trend_desc_volume)
            Density -> AppTokens.strings.res(Res.string.performance_trend_desc_density)
            Repetitions -> AppTokens.strings.res(Res.string.performance_trend_desc_repetitions)
            Intensity -> AppTokens.strings.res(Res.string.performance_trend_desc_intensity)
        }
    }

    @Composable
    public fun label(): String {
        return when (this) {
            Duration -> AppTokens.strings.res(Res.string.duration)
            Volume -> AppTokens.strings.res(Res.string.volume)
            Density -> AppTokens.strings.res(Res.string.performance_metric_density)
            Repetitions -> AppTokens.strings.res(Res.string.repetitions)
            Intensity -> AppTokens.strings.res(Res.string.performance_metric_intensity)
        }
    }
}

@Immutable
public data class PerformanceTrendHistoryEntry(
    val endDate: DateTimeFormatState,
    val metric: PerformanceMetricState,
)

@Immutable
public sealed interface PerformanceMetricState {
    public val type: PerformanceMetricTypeState
    public val deltaPercentage: Int
    public val currentVsAveragePercentage: Int
    public val status: PerformanceTrendStatusState

    @Immutable
    public data class Volume(
        override val deltaPercentage: Int,
        override val currentVsAveragePercentage: Int,
        val current: VolumeFormatState,
        val average: VolumeFormatState,
        val best: VolumeFormatState,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Volume
    }

    @Immutable
    public data class Density(
        override val deltaPercentage: Int,
        override val currentVsAveragePercentage: Int,
        val current: DensityFormatState,
        val average: DensityFormatState,
        val best: DensityFormatState,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Density
    }

    @Immutable
    public data class Duration(
        override val deltaPercentage: Int,
        override val currentVsAveragePercentage: Int,
        val current: DurationFormatState,
        val average: DurationFormatState,
        val best: DurationFormatState,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Duration
    }

    @Immutable
    public data class Repetitions(
        override val deltaPercentage: Int,
        override val currentVsAveragePercentage: Int,
        val current: RepetitionsFormatState,
        val average: RepetitionsFormatState,
        val best: RepetitionsFormatState,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Repetitions
    }

    @Immutable
    public data class Intensity(
        override val deltaPercentage: Int,
        override val currentVsAveragePercentage: Int,
        val current: IntensityFormatState,
        val average: IntensityFormatState,
        val best: IntensityFormatState,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Intensity
    }
}

@Immutable
public enum class PerformanceTrendStatusState {
    Record,
    Improved,
    Stable,
    Declined
}

public fun stubPerformanceTrendHistory(): ImmutableList<PerformanceTrendHistoryEntry> {
    val now = DateTimeUtils.now()
    val anyRange = DateRangePresets.infinity()
    return persistentListOf(
        PerformanceTrendHistoryEntry(
            endDate = DateTimeFormatState.of(
                value = now,
                range = anyRange,
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            metric = stubPerformanceMetrics().random()
        ),
        PerformanceTrendHistoryEntry(
            endDate = DateTimeFormatState.of(
                value = now,
                range = anyRange,
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            metric = stubPerformanceMetrics().random()
        ),
        PerformanceTrendHistoryEntry(
            endDate = DateTimeFormatState.of(
                value = now,
                range = anyRange,
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            metric = stubPerformanceMetrics().random()
        ),
    )
}

public fun stubPerformanceMetrics(): ImmutableList<PerformanceMetricState> {
    return persistentListOf(
        PerformanceMetricState.Volume(
            deltaPercentage = 24,
            currentVsAveragePercentage = 33,
            current = VolumeFormatState.of(1_200f),
            average = VolumeFormatState.of(900f),
            best = VolumeFormatState.of(1_200f),
            status = PerformanceTrendStatusState.Record
        ),
        PerformanceMetricState.Density(
            deltaPercentage = 10,
            currentVsAveragePercentage = 10,
            current = DensityFormatState.of(22f),
            average = DensityFormatState.of(20f),
            best = DensityFormatState.of(24f),
            status = PerformanceTrendStatusState.Improved
        ),
        PerformanceMetricState.Volume(
            deltaPercentage = -12,
            currentVsAveragePercentage = -12,
            current = VolumeFormatState.of(780f),
            average = VolumeFormatState.of(890f),
            best = VolumeFormatState.of(1_050f),
            status = PerformanceTrendStatusState.Declined
        ),
        PerformanceMetricState.Duration(
            deltaPercentage = 0,
            currentVsAveragePercentage = -7,
            current = DurationFormatState.of(65.minutes),
            average = DurationFormatState.of(70.minutes),
            best = DurationFormatState.of(80.minutes),
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Repetitions(
            deltaPercentage = 12,
            currentVsAveragePercentage = 12,
            current = RepetitionsFormatState.of(84),
            average = RepetitionsFormatState.of(75),
            best = RepetitionsFormatState.of(92),
            status = PerformanceTrendStatusState.Improved
        ),
        PerformanceMetricState.Repetitions(
            deltaPercentage = 0,
            currentVsAveragePercentage = 0,
            current = RepetitionsFormatState.of(70),
            average = RepetitionsFormatState.of(70),
            best = RepetitionsFormatState.of(85),
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Intensity(
            deltaPercentage = 3,
            currentVsAveragePercentage = 3,
            current = IntensityFormatState.of(36f),
            average = IntensityFormatState.of(35f),
            best = IntensityFormatState.of(42f),
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Intensity(
            deltaPercentage = 9,
            currentVsAveragePercentage = 18,
            current = IntensityFormatState.of(40f),
            average = IntensityFormatState.of(34f),
            best = IntensityFormatState.of(43f),
            status = PerformanceTrendStatusState.Record
        )
    )
}
