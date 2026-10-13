package com.grippo.goal.setup.suggestion

import com.grippo.analytics.goal.setup.suggestion.GoalSetupSuggestionAnalytics
import com.grippo.analytics.goal.setup.suggestion.models.GoalSetupSuggestionEvent
import com.grippo.core.foundation.BaseViewModel

public class GoalSetupSuggestionViewModel(
    private val analytics: GoalSetupSuggestionAnalytics,
) :
    BaseViewModel<GoalSetupSuggestionState, GoalSetupSuggestionDirection, GoalSetupSuggestionLoader>(
        GoalSetupSuggestionState()
    ),
    GoalSetupSuggestionContract {

    override fun onConfigure() {
        analytics.trackEvent(GoalSetupSuggestionEvent.Configure)
        navigateTo(GoalSetupSuggestionDirection.Configure)
    }

    override fun onLater() {
        analytics.trackEvent(GoalSetupSuggestionEvent.Later)
        navigateTo(GoalSetupSuggestionDirection.Later)
    }

    override fun onBack() {
        analytics.trackEvent(GoalSetupSuggestionEvent.DismissRequested)
        navigateTo(GoalSetupSuggestionDirection.Back)
    }
}
