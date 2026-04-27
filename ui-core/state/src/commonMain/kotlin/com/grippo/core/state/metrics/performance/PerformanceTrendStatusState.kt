package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Immutable

@Immutable
public enum class PerformanceTrendStatusState {
    Record,
    Improved,
    Stable,
    Declined
}
