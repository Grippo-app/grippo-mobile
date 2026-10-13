package com.grippo.analytics.exercise.example

import com.grippo.analytics.exercise.example.models.ExerciseExampleEvent
import com.grippo.analytics.exercise.example.models.ExerciseExampleOperation

public interface ExerciseExampleAnalytics {
    public fun trackEvent(event: ExerciseExampleEvent)
    public suspend fun <T> trackOperation(operation: ExerciseExampleOperation, block: suspend () -> T): T
}
