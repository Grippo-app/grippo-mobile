package com.grippo.analytics.duration.picker.models

public sealed interface DurationPickerEvent {
    public data object SubmitClicked : DurationPickerEvent
    public data object DismissRequested : DurationPickerEvent
}
