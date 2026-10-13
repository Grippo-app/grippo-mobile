package com.grippo.analytics.period.picker

import com.grippo.analytics.period.picker.models.PeriodPickerEvent

public interface PeriodPickerAnalytics {
    public fun trackEvent(event: PeriodPickerEvent)
}
