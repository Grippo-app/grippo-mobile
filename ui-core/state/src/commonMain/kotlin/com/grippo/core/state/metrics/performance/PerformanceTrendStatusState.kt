package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.performance_status_declined
import com.grippo.design.resources.provider.performance_status_declined_previous
import com.grippo.design.resources.provider.performance_status_improved
import com.grippo.design.resources.provider.performance_status_improved_previous
import com.grippo.design.resources.provider.performance_status_record
import com.grippo.design.resources.provider.performance_status_stable
import com.grippo.design.resources.provider.performance_status_stable_previous

@Immutable
public enum class PerformanceTrendStatusState {
    Empty,
    Record,
    Improved,
    Stable,
    Declined;

    @Composable
    public fun label(baseline: PerformanceBaselineState): String? = when (this) {
        Record -> AppTokens.strings.res(Res.string.performance_status_record)
        Improved -> when (baseline) {
            PerformanceBaselineState.Average -> AppTokens.strings.res(Res.string.performance_status_improved)
            PerformanceBaselineState.Previous -> AppTokens.strings.res(Res.string.performance_status_improved_previous)
        }

        Stable -> when (baseline) {
            PerformanceBaselineState.Average -> AppTokens.strings.res(Res.string.performance_status_stable)
            PerformanceBaselineState.Previous -> AppTokens.strings.res(Res.string.performance_status_stable_previous)
        }

        Declined -> when (baseline) {
            PerformanceBaselineState.Average -> AppTokens.strings.res(Res.string.performance_status_declined)
            PerformanceBaselineState.Previous -> AppTokens.strings.res(Res.string.performance_status_declined_previous)
        }

        Empty -> null
    }

    @Composable
    public fun backgroundColor(): Color = when (this) {
        Record, Improved -> AppTokens.colors.semantic.successBackground
        Stable, Empty -> AppTokens.colors.background.screen
        Declined -> AppTokens.colors.semantic.declineBackground
    }
}
