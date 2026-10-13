package com.grippo.analytics.month.picker.internal.schema

import com.grippo.analytics.month.picker.models.MonthPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object MonthPickerEventMatrix {
    private val SubmitClicked = AnalyticsEventSchema("month_picker_submit_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("month_picker_dismiss_requested", 1, emptyList())

    val all = listOf(SubmitClicked, DismissRequested)

    fun event(event: MonthPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            MonthPickerEvent.SubmitClicked -> SubmitClicked
            MonthPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
