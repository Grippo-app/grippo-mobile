package com.grippo.analytics.confirmation

import com.grippo.analytics.confirmation.models.ConfirmationEvent

public interface ConfirmationAnalytics {
    public fun trackEvent(event: ConfirmationEvent)
}
