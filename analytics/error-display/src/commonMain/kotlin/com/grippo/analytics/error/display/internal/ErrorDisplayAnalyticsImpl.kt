package com.grippo.analytics.error.display.internal

import com.grippo.analytics.error.display.ErrorDisplayAnalytics
import com.grippo.analytics.error.display.internal.schema.ErrorDisplayEventMatrix
import com.grippo.analytics.error.display.models.ErrorDisplayEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [ErrorDisplayAnalytics::class])
internal class ErrorDisplayAnalyticsImpl(private val analytics: Analytics) : ErrorDisplayAnalytics {
    override fun trackEvent(event: ErrorDisplayEvent) {
        try {
            analytics.track(ErrorDisplayEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block error dismissal.
        }
    }
}
