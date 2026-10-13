package com.grippo.analytics.confirmation.internal

import com.grippo.analytics.confirmation.ConfirmationAnalytics
import com.grippo.analytics.confirmation.internal.schema.ConfirmationEventMatrix
import com.grippo.analytics.confirmation.models.ConfirmationEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [ConfirmationAnalytics::class])
internal class ConfirmationAnalyticsImpl(private val analytics: Analytics) : ConfirmationAnalytics {
    override fun trackEvent(event: ConfirmationEvent) {
        try {
            analytics.track(ConfirmationEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block confirmation or returning to the caller.
        }
    }
}
