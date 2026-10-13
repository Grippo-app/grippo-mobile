package com.grippo.analytics.performance.trend.details.internal.schema

import com.grippo.analytics.performance.trend.details.models.PerformanceTrendDetailsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object PerformanceTrendDetailsEventMatrix {
    private val DismissRequested = AnalyticsEventSchema("performance_trend_details_dismiss", 1, emptyList())

    val all = listOf(DismissRequested)

    fun event(event: PerformanceTrendDetailsEvent): AnalyticsEvent = Payload(
        when (event) {
            PerformanceTrendDetailsEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
