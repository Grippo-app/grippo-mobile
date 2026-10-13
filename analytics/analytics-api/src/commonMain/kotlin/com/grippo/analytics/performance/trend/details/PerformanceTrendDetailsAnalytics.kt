package com.grippo.analytics.performance.trend.details

import com.grippo.analytics.performance.trend.details.models.PerformanceTrendDetailsEvent

public interface PerformanceTrendDetailsAnalytics {
    public fun trackEvent(event: PerformanceTrendDetailsEvent)
}
