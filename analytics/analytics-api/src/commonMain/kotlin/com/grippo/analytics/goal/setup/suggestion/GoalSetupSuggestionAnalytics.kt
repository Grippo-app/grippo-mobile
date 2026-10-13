package com.grippo.analytics.goal.setup.suggestion

import com.grippo.analytics.goal.setup.suggestion.models.GoalSetupSuggestionEvent

public interface GoalSetupSuggestionAnalytics {
    public fun trackEvent(event: GoalSetupSuggestionEvent)
}
