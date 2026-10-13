package com.grippo.analytics.weight.picker

import com.grippo.analytics.weight.picker.models.WeightPickerEvent

public interface WeightPickerAnalytics {
    public fun trackEvent(event: WeightPickerEvent)
}
