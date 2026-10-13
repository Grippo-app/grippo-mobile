package com.grippo.analytics.iteration.picker

import com.grippo.analytics.iteration.picker.models.IterationPickerEvent

public interface IterationPickerAnalytics {
    public fun trackEvent(event: IterationPickerEvent)
}
