package com.grippo.core.state.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.icons.Intensity
import com.grippo.design.resources.provider.icons.Repeat
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.icons.Volume
import kotlin.time.Duration.Companion.minutes

@Immutable
public enum class PerformanceMetricTypeState {
    Duration,
    Volume,
    Repetitions,
    Intensity;

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            Duration -> AppTokens.icons.Timer
            Volume -> AppTokens.icons.Volume
            Repetitions -> AppTokens.icons.Repeat
            Intensity -> AppTokens.icons.Intensity
        }
    }
}

@Immutable
public sealed interface PerformanceMetricState {
    public val type: PerformanceMetricTypeState
    public val deltaPercentage: Int
    public val status: PerformanceTrendStatusState

    @Immutable
    public data class Volume(
        override val deltaPercentage: Int,
        val current: VolumeFormatState,
        val average: VolumeFormatState,
        val best: VolumeFormatState,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Volume
    }

    @Immutable
    public data class Duration(
        override val deltaPercentage: Int,
        val current: kotlin.time.Duration,
        val average: kotlin.time.Duration,
        val best: kotlin.time.Duration,
        override val status: PerformanceTrendStatusState,
    ) : PerformanceMetricState {
        override val type: PerformanceMetricTypeState = PerformanceMetricTypeState.Duration
    }

    @Immutable
    public data class Repetitions(
        override val deltaPercentage: Int,
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

public fun stubPerformanceMetrics(): List<PerformanceMetricState> {
    return listOf(
        PerformanceMetricState.Volume(
            deltaPercentage = 24,
            current = VolumeFormatState.of(1_200f),
            average = VolumeFormatState.of(900f),
            best = VolumeFormatState.of(1_200f),
            status = PerformanceTrendStatusState.Record
        ),
        PerformanceMetricState.Volume(
            deltaPercentage = -12,
            current = VolumeFormatState.of(780f),
            average = VolumeFormatState.of(890f),
            best = VolumeFormatState.of(1_050f),
            status = PerformanceTrendStatusState.Declined
        ),
        PerformanceMetricState.Duration(
            deltaPercentage = -8,
            current = 65.minutes,
            average = 70.minutes,
            best = 75.minutes,
            status = PerformanceTrendStatusState.Declined
        ),
        PerformanceMetricState.Duration(
            deltaPercentage = 15,
            current = 72.minutes,
            average = 68.minutes,
            best = 80.minutes,
            status = PerformanceTrendStatusState.Improved
        ),
        PerformanceMetricState.Repetitions(
            deltaPercentage = 12,
            current = RepetitionsFormatState.of(84),
            average = RepetitionsFormatState.of(75),
            best = RepetitionsFormatState.of(92),
            status = PerformanceTrendStatusState.Improved
        ),
        PerformanceMetricState.Repetitions(
            deltaPercentage = 0,
            current = RepetitionsFormatState.of(70),
            average = RepetitionsFormatState.of(70),
            best = RepetitionsFormatState.of(85),
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Intensity(
            deltaPercentage = 3,
            current = IntensityFormatState.of(36f),
            average = IntensityFormatState.of(35f),
            best = IntensityFormatState.of(42f),
            status = PerformanceTrendStatusState.Stable
        ),
        PerformanceMetricState.Intensity(
            deltaPercentage = 9,
            current = IntensityFormatState.of(40f),
            average = IntensityFormatState.of(34f),
            best = IntensityFormatState.of(43f),
            status = PerformanceTrendStatusState.Record
        )
    )
}
