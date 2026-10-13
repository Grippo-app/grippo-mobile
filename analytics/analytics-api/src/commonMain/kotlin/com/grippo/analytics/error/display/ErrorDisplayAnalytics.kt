package com.grippo.analytics.error.display

import com.grippo.analytics.error.display.models.ErrorDisplayEvent

public interface ErrorDisplayAnalytics {
    public fun trackEvent(event: ErrorDisplayEvent)
}
