package com.grippo.analytics.period.picker.models

public sealed interface PeriodPickerEvent {
    public data object RangeSelected : PeriodPickerEvent
    public data object DismissRequested : PeriodPickerEvent
}
