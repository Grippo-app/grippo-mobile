package com.grippo.analytics.weight.picker.models

public sealed interface WeightPickerEvent {
    public data object SelectionChanged : WeightPickerEvent
    public data object SubmitClicked : WeightPickerEvent
    public data object DismissRequested : WeightPickerEvent
}
