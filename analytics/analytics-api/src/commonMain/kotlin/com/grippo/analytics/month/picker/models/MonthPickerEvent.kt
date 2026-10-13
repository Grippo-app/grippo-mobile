package com.grippo.analytics.month.picker.models

public sealed interface MonthPickerEvent {
    public data object SubmitClicked : MonthPickerEvent
    public data object DismissRequested : MonthPickerEvent
}
