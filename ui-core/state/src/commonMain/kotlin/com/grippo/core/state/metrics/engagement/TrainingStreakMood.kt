package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.AppColor

@Immutable
public enum class TrainingStreakMood {
    CrushingIt,
    OnTrack,
    Paused,
    Restart;

    @Composable
    public fun indicatorColors(): AppColor.Charts.IndicatorColors.IndicatorColors = when (this) {
        CrushingIt -> AppTokens.colors.charts.indicator.success
        OnTrack -> AppTokens.colors.charts.indicator.info
        Paused -> AppTokens.colors.charts.indicator.muted
        Restart -> AppTokens.colors.charts.indicator.warning
    }
}
