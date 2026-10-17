package com.grippo.analytics.exercise

import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.analytics.exercise.models.ExerciseOperation

public interface ExerciseAnalytics {
    public fun trackEvent(event: ExerciseEvent)
    public suspend fun <T> trackOperation(operation: ExerciseOperation, block: suspend () -> T): T
}
