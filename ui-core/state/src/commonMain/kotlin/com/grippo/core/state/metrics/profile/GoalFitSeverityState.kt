package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable

@Immutable
public enum class GoalFitSeverityState(
    /** Sort order — FAIL first (most actionable), PASS last. */
    public val sortKey: Int,
) {
    PASS(sortKey = 2),
    WARN(sortKey = 1),
    FAIL(sortKey = 0)
}
