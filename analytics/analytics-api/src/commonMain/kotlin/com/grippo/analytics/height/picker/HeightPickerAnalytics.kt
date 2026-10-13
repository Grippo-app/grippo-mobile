package com.grippo.analytics.height.picker

import com.grippo.analytics.height.picker.models.HeightPickerEvent

public interface HeightPickerAnalytics {
    public fun trackEvent(event: HeightPickerEvent)
}
