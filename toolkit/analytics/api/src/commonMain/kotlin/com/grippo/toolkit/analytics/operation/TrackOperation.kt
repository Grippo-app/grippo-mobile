package com.grippo.toolkit.analytics.operation

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.internal.executeTrackedOperation
import kotlin.time.TimeSource

@Throws(Exception::class)
public suspend fun <T> Analytics.trackOperation(
    events: AnalyticsOperationEvents<T>,
    block: suspend () -> T,
): T {
    return executeTrackedOperation(events, TimeSource.Monotonic, block)
}
