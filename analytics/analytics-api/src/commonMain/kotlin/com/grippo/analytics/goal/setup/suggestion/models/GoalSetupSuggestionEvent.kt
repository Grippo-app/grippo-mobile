package com.grippo.analytics.goal.setup.suggestion.models

public sealed interface GoalSetupSuggestionEvent {
    public data object Configure : GoalSetupSuggestionEvent
    public data object Later : GoalSetupSuggestionEvent
    public data object DismissRequested : GoalSetupSuggestionEvent
}
