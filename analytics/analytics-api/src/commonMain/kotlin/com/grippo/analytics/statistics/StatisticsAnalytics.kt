package com.grippo.analytics.statistics

import com.grippo.analytics.statistics.models.StatisticsEvent
import com.grippo.analytics.statistics.models.StatisticsOperation

public interface StatisticsAnalytics {
    public fun trackEvent(event: StatisticsEvent)
    public suspend fun <T> trackOperation(operation: StatisticsOperation, block: suspend () -> T): T
}
