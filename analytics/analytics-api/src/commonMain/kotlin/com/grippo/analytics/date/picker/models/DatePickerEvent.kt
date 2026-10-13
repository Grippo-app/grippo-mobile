package com.grippo.analytics.date.picker.models

public sealed interface DatePickerEvent {
    public data object Submit : DatePickerEvent
    public data object Dismiss : DatePickerEvent
}
