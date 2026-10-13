package com.grippo.analytics.statistics.models

public sealed interface StatisticsEvent {
    public data object DismissRequested : StatisticsEvent
}
