package com.grippo.analytics.statistics.internal

import com.grippo.analytics.statistics.StatisticsAnalytics
import com.grippo.analytics.statistics.internal.operation.StatisticsOperationEvents
import com.grippo.analytics.statistics.internal.schema.StatisticsEventMatrix
import com.grippo.analytics.statistics.models.StatisticsEvent
import com.grippo.analytics.statistics.models.StatisticsOperation
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [StatisticsAnalytics::class])
internal class StatisticsAnalyticsImpl(private val analytics: Analytics) : StatisticsAnalytics {
    override fun trackEvent(event: StatisticsEvent) {
        try {
            analytics.track(StatisticsEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }

    override suspend fun <T> trackOperation(operation: StatisticsOperation, block: suspend () -> T): T {
        return analytics.trackOperation(StatisticsOperationEvents<T>(operation), block = block)
    }
}
