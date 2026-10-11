package com.grippo.toolkit.analytics.internal

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.values.DurationMillis
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun <T> Analytics.executeTrackedOperation(
    events: AnalyticsOperationEvents<T>,
    flow: FlowContext,
    timeSource: TimeSource,
    block: suspend () -> T,
): T {
    currentCoroutineContext().ensureActive()
    val operation = OperationId.create()
    track(events.started(), flow, operation)
    val started = timeSource.markNow()
    val result = try {
        currentCoroutineContext().ensureActive()
        block().also { currentCoroutineContext().ensureActive() }
    } catch (error: Exception) {
        // A schema/encoding defect must not replace the original operation failure.
        try {
            val duration = DurationMillis(started.elapsedNow().inWholeMilliseconds)
            val event = if (error is CancellationException) events.interrupted(duration)
            else events.failed(error, duration)
            track(event, flow, operation)
        } catch (reportingError: Exception) {
            if (reportingError !== error) error.addSuppressed(reportingError)
        }
        throw error
    }
    // Keep encoding outside the operation catch: its defect cannot produce two terminal events.
    track(events.completed(result, DurationMillis(started.elapsedNow().inWholeMilliseconds)), flow, operation)
    return result
}
