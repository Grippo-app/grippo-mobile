package com.grippo.analytics.primary.goal.picker.models

public sealed interface PrimaryGoalPickerEvent {
    public data object GoalSelected : PrimaryGoalPickerEvent
    public data object DismissRequested : PrimaryGoalPickerEvent
}
