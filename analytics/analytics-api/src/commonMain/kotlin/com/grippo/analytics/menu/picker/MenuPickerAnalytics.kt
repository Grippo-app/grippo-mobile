package com.grippo.analytics.menu.picker

import com.grippo.analytics.menu.picker.models.MenuPickerEvent

public interface MenuPickerAnalytics {
    public fun trackEvent(event: MenuPickerEvent)
}
