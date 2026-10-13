package com.grippo.analytics.primary.goal.picker

import com.grippo.analytics.primary.goal.picker.models.PrimaryGoalPickerEvent

public interface PrimaryGoalPickerAnalytics {
    public fun trackEvent(event: PrimaryGoalPickerEvent)
}
