package com.grippo.analytics.training.goal.details

import com.grippo.analytics.training.goal.details.models.TrainingGoalDetailsEvent

public interface TrainingGoalDetailsAnalytics {
    public fun trackEvent(event: TrainingGoalDetailsEvent)
}
