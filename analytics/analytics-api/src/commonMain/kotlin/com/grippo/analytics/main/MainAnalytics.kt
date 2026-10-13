package com.grippo.analytics.main

import com.grippo.analytics.main.models.MainEvent

public interface MainAnalytics {
    public fun trackEvent(event: MainEvent)
}
