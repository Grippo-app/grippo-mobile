package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens

@Immutable
public enum class GoalFitSeverityState(
    /** Sort order — FAIL first (most actionable), PASS last. */
    public val sortKey: Int,
) {
    PASS(sortKey = 2),
    WARN(sortKey = 1),
    FAIL(sortKey = 0);

    @Composable
    public fun dotColor(): Color = when (this) {
        PASS -> AppTokens.colors.semantic.success
        WARN -> AppTokens.colors.semantic.warning
        FAIL -> AppTokens.colors.semantic.error
    }

    @Composable
    public fun valueColor(): Color = when (this) {
        FAIL -> AppTokens.colors.semantic.error
        WARN, PASS -> AppTokens.colors.text.primary
    }
}
