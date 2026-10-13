package com.grippo.analytics.period.picker.internal.schema

import com.grippo.analytics.period.picker.models.PeriodPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object PeriodPickerEventMatrix {
    private val RangeSelected = AnalyticsEventSchema("period_picker_range_selected", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("period_picker_dismiss_requested", 1, emptyList())

    val all = listOf(RangeSelected, DismissRequested)

    fun event(event: PeriodPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            PeriodPickerEvent.RangeSelected -> RangeSelected
            PeriodPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
