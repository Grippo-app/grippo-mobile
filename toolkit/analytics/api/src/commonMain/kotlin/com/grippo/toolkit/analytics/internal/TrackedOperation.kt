package com.grippo.toolkit.analytics.internal

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.values.DurationMillis
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException

internal suspend fun <T> Analytics.executeTrackedOperation(
    events: AnalyticsOperationEvents<T>,
    timeSource: TimeSource,
    block: suspend () -> T,
): T {
    val operation = OperationId.create()
    try {
        track(events.started(), operation)
    } catch (_: Exception) {
        // Reporting must not prevent business execution.
    }
    val started = timeSource.markNow()
    val result = try {
        block()
    } catch (error: Exception) {
        try {
            val duration = DurationMillis(started.elapsedNow().inWholeMilliseconds)
            val event = if (error is CancellationException) events.interrupted(duration)
            else events.failed(error, duration)
            track(event, operation)
        } catch (reportingError: Exception) {
            if (reportingError !== error) error.addSuppressed(reportingError)
        }
        throw error
    }
    // Keep encoding outside the operation catch: its defect cannot produce two terminal events.
    try {
        track(events.completed(result, DurationMillis(started.elapsedNow().inWholeMilliseconds)), operation)
    } catch (_: Exception) {
        // Reporting must not replace the successful business result.
    }
    return result
}
