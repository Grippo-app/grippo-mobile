package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Flame
import com.grippo.design.resources.provider.icons.Gauge
import com.grippo.design.resources.provider.icons.Repeat
import com.grippo.design.resources.provider.icons.Stack
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.performance_metric_density
import com.grippo.design.resources.provider.performance_metric_intensity
import com.grippo.design.resources.provider.performance_trend_desc_density
import com.grippo.design.resources.provider.performance_trend_desc_duration
import com.grippo.design.resources.provider.performance_trend_desc_intensity
import com.grippo.design.resources.provider.performance_trend_desc_repetitions
import com.grippo.design.resources.provider.performance_trend_desc_volume
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.volume
import kotlinx.serialization.Serializable

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
            Volume -> AppTokens.icons.Stack
            Density -> AppTokens.icons.Gauge
            Repetitions -> AppTokens.icons.Repeat
            Intensity -> AppTokens.icons.Flame
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
