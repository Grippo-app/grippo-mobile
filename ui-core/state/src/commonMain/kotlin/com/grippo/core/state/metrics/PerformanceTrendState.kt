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
