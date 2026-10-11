package com.grippo.toolkit.analytics.operation

import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.values.DurationMillis

/** Feature-owned, pure event factories. Never encode exception messages or credentials. */
public interface AnalyticsOperationEvents<in T> {
    public fun started(): AnalyticsEvent
    /** A returned value is not necessarily business success: classify typed results here. */
    public fun completed(result: T, duration: DurationMillis): AnalyticsEvent
    public fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent
    /** Coroutine interruption, distinct from an explicit provider/user cancellation. */
    public fun interrupted(duration: DurationMillis): AnalyticsEvent
}

