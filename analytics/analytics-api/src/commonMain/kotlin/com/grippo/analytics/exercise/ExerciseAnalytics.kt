package com.grippo.analytics.exercise

import com.grippo.analytics.exercise.models.ExerciseEvent

public interface ExerciseAnalytics {
    public fun trackEvent(event: ExerciseEvent)
}
