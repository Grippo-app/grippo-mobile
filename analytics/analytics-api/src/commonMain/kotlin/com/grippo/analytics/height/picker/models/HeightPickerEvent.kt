package com.grippo.analytics.height.picker.models

public sealed interface HeightPickerEvent {
    public data object SubmitClicked : HeightPickerEvent
    public data object DismissRequested : HeightPickerEvent
}
