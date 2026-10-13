package com.grippo.analytics.date.picker.internal.events

import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal class DatePickerPayloadEvent(
    override val schema: AnalyticsEventSchema,
) : AnalyticsEvent {
    override fun parameters(): List<AnalyticsParameter> = emptyList()
}
