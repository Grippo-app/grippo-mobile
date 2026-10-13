package com.grippo.analytics.performance.trend.details.models

public sealed interface PerformanceTrendDetailsEvent {
    public data object DismissRequested : PerformanceTrendDetailsEvent
}
