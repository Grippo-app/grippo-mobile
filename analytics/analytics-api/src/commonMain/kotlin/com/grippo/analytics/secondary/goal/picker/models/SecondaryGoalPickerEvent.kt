package com.grippo.analytics.secondary.goal.picker.models

public sealed interface SecondaryGoalPickerEvent {
    public data object GoalClicked : SecondaryGoalPickerEvent
    public data object DismissRequested : SecondaryGoalPickerEvent
}
