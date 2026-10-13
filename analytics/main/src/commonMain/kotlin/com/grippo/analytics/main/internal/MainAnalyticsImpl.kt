package com.grippo.analytics.main.internal

import com.grippo.analytics.main.MainAnalytics
import com.grippo.analytics.main.internal.schema.MainEventMatrix
import com.grippo.analytics.main.models.MainEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [MainAnalytics::class])
internal class MainAnalyticsImpl(private val analytics: Analytics) : MainAnalytics {
    override fun trackEvent(event: MainEvent) {
        try {
            analytics.track(MainEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }
}
