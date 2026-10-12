package com.grippo.toolkit.analytics.operation

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.internal.executeTrackedOperation
import kotlin.time.TimeSource

/**
 * A started operation has one classified terminal outcome; both share a fresh ID.
 * An already-cancelled caller starts no work. Reporting exceptions never prevent the block or replace its result.
 * Fatal errors are not business outcomes.
 * Call inside the existing safeLaunch/use-case coroutine. No new job, retry or dispatcher.
 * Exceptions retain their identity and are rethrown to the existing error handler.
 */
@Throws(Exception::class)
public suspend fun <T> Analytics.trackOperation(
    events: AnalyticsOperationEvents<T>,
    flow: FlowContext,
    block: suspend () -> T,
): T {
    return executeTrackedOperation(events, flow, TimeSource.Monotonic, block)
}
