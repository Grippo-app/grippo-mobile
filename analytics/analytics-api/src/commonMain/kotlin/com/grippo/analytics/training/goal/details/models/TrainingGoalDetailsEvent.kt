package com.grippo.analytics.training.goal.details.models

public sealed interface TrainingGoalDetailsEvent {
    public data object AddGoalClicked : TrainingGoalDetailsEvent
    public data object DismissRequested : TrainingGoalDetailsEvent
}
