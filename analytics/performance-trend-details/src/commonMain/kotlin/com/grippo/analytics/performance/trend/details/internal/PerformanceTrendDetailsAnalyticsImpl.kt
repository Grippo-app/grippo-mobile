package com.grippo.analytics.performance.trend.details.internal

import com.grippo.analytics.performance.trend.details.PerformanceTrendDetailsAnalytics
import com.grippo.analytics.performance.trend.details.internal.schema.PerformanceTrendDetailsEventMatrix
import com.grippo.analytics.performance.trend.details.models.PerformanceTrendDetailsEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [PerformanceTrendDetailsAnalytics::class])
internal class PerformanceTrendDetailsAnalyticsImpl(private val analytics: Analytics) : PerformanceTrendDetailsAnalytics {
    override fun trackEvent(event: PerformanceTrendDetailsEvent) {
        try {
            analytics.track(PerformanceTrendDetailsEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
