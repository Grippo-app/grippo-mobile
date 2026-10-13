package com.grippo.analytics.duration.picker

import com.grippo.analytics.duration.picker.models.DurationPickerEvent

public interface DurationPickerAnalytics {
    public fun trackEvent(event: DurationPickerEvent)
}
