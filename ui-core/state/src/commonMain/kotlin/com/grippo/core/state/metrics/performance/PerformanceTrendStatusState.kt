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
    public fun label(reference: PerformanceReferenceState): String? = when (this) {
        Record -> AppTokens.strings.res(Res.string.performance_status_record)
        Improved -> when (reference) {
            PerformanceReferenceState.Average -> AppTokens.strings.res(Res.string.performance_status_improved)
            PerformanceReferenceState.Previous -> AppTokens.strings.res(Res.string.performance_status_improved_previous)
        }

        Stable -> when (reference) {
            PerformanceReferenceState.Average -> AppTokens.strings.res(Res.string.performance_status_stable)
            PerformanceReferenceState.Previous -> AppTokens.strings.res(Res.string.performance_status_stable_previous)
        }

        Declined -> when (reference) {
            PerformanceReferenceState.Average -> AppTokens.strings.res(Res.string.performance_status_declined)
            PerformanceReferenceState.Previous -> AppTokens.strings.res(Res.string.performance_status_declined_previous)
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
