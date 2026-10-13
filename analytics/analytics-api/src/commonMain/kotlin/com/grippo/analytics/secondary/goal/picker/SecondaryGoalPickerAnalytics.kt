package com.grippo.analytics.secondary.goal.picker

import com.grippo.analytics.secondary.goal.picker.models.SecondaryGoalPickerEvent

public interface SecondaryGoalPickerAnalytics {
    public fun trackEvent(event: SecondaryGoalPickerEvent)
}
