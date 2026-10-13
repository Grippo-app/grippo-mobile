package com.grippo.analytics.training.streak.details

import com.grippo.analytics.training.streak.details.models.TrainingStreakDetailsEvent

public interface TrainingStreakDetailsAnalytics {
    public fun trackEvent(event: TrainingStreakDetailsEvent)
}
