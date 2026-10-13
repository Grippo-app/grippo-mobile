package com.grippo.analytics.date.picker

import com.grippo.analytics.date.picker.models.DatePickerEvent

public interface DatePickerAnalytics {
    public fun trackEvent(event: DatePickerEvent)
}
