package com.grippo.analytics.statistics.internal.schema

import com.grippo.analytics.statistics.models.StatisticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object StatisticsEventMatrix {
    private val DismissRequested = AnalyticsEventSchema("statistics_dismiss", 1, emptyList())

    val all = listOf(DismissRequested)

    fun event(event: StatisticsEvent): AnalyticsEvent = Payload(
        when (event) {
            StatisticsEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
