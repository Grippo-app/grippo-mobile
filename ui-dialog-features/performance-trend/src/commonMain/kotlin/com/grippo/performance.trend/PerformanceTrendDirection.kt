package com.grippo.performance.trend

import com.grippo.core.foundation.models.BaseDirection

public sealed interface PerformanceTrendDirection : BaseDirection {
    public data object Back : PerformanceTrendDirection
}
