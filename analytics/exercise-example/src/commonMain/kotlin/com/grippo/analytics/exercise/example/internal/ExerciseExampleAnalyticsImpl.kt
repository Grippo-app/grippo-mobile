package com.grippo.analytics.exercise.example.internal

import com.grippo.analytics.exercise.example.ExerciseExampleAnalytics
import com.grippo.analytics.exercise.example.internal.operation.ExerciseExampleOperationEvents
import com.grippo.analytics.exercise.example.internal.schema.ExerciseExampleEventMatrix
import com.grippo.analytics.exercise.example.models.ExerciseExampleEvent
import com.grippo.analytics.exercise.example.models.ExerciseExampleOperation
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleAnalytics::class])
internal class ExerciseExampleAnalyticsImpl(private val analytics: Analytics) : ExerciseExampleAnalytics {
    override fun trackEvent(event: ExerciseExampleEvent) {
        try {
            analytics.track(ExerciseExampleEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: ExerciseExampleOperation, block: suspend () -> T): T {
        return analytics.trackOperation(ExerciseExampleOperationEvents<T>(operation), block = block)
    }
}
