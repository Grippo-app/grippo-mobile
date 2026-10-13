package com.grippo.analytics.date.picker.models

public sealed interface DatePickerEvent {
    public data object SubmitClicked : DatePickerEvent
    public data object DismissRequested : DatePickerEvent
}
