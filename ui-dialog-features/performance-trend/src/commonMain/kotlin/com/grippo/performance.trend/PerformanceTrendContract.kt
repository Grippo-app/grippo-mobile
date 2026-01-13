package com.grippo.performance.trend

import androidx.compose.runtime.Immutable

@Immutable
public interface PerformanceTrendContract {
    public fun onBack()

    public companion object Empty : PerformanceTrendContract {
        override fun onBack() {}
    }
}
