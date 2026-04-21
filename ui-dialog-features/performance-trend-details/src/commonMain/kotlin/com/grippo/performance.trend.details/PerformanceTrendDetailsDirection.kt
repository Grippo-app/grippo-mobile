package com.grippo.performance.trend.details

import com.grippo.core.foundation.models.BaseDirection

public sealed interface PerformanceTrendDetailsDirection : BaseDirection {
    public data object Back : PerformanceTrendDetailsDirection
}
