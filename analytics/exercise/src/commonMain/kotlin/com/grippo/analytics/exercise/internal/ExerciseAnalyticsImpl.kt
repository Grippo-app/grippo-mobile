package com.grippo.analytics.exercise.internal

import com.grippo.analytics.exercise.ExerciseAnalytics
import com.grippo.analytics.exercise.internal.operation.ExerciseOperationEvents
import com.grippo.analytics.exercise.internal.schema.ExerciseEventMatrix
import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.analytics.exercise.models.ExerciseOperation
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [ExerciseAnalytics::class])
internal class ExerciseAnalyticsImpl(private val analytics: Analytics) : ExerciseAnalytics {
    override fun trackEvent(event: ExerciseEvent) {
        try {
            analytics.track(ExerciseEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: ExerciseOperation, block: suspend () -> T): T {
        return analytics.trackOperation(events = ExerciseOperationEvents<T>(operation), block = block)
    }
}
