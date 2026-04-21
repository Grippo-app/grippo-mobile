package com.grippo.performance.trend.details

import androidx.compose.runtime.Immutable

@Immutable
public interface PerformanceTrendDetailsContract {
    public fun onBack()

    public companion object Empty : PerformanceTrendDetailsContract {
        override fun onBack() {}
    }
}
