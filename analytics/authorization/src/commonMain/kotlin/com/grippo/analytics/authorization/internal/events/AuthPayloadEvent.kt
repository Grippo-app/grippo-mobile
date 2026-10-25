package com.grippo.analytics.authorization.internal.events

import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal class AuthPayloadEvent(
    override val schema: AnalyticsEventSchema,
    private val parameters: List<AnalyticsParameter>,
) : AnalyticsEvent {
    override fun parameters(): List<AnalyticsParameter> = parameters
}
