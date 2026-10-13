package com.grippo.analytics.month.picker

import com.grippo.analytics.month.picker.models.MonthPickerEvent

public interface MonthPickerAnalytics {
    public fun trackEvent(event: MonthPickerEvent)
}
