package com.grippo.goal.setup.suggestion

import com.grippo.core.foundation.BaseViewModel

public class GoalSetupSuggestionViewModel :
    BaseViewModel<GoalSetupSuggestionState, GoalSetupSuggestionDirection, GoalSetupSuggestionLoader>(
        GoalSetupSuggestionState()
    ),
    GoalSetupSuggestionContract {

    override fun onConfigure() {
        navigateTo(GoalSetupSuggestionDirection.Configure)
    }

    override fun onLater() {
        navigateTo(GoalSetupSuggestionDirection.Later)
    }

    override fun onBack() {
        navigateTo(GoalSetupSuggestionDirection.Back)
    }
}
