package com.grippo.toolkit.analytics.operation

import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.values.DurationMillis

public interface AnalyticsOperationEvents<in T> {
    public fun started(): AnalyticsEvent
    public fun completed(result: T, duration: DurationMillis): AnalyticsEvent
    public fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent
    public fun interrupted(duration: DurationMillis): AnalyticsEvent
}

