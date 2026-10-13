package com.grippo.analytics.date.picker.internal.schema

import com.grippo.analytics.date.picker.internal.events.DatePickerPayloadEvent
import com.grippo.analytics.date.picker.models.DatePickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema

internal object DatePickerEventMatrix {
    private val Submit = AnalyticsEventSchema("date_picker_submit", 1, emptyList())
    private val Dismiss = AnalyticsEventSchema("date_picker_dismiss", 1, emptyList())

    val all: List<AnalyticsEventSchema> = listOf(Submit, Dismiss)

    fun event(event: DatePickerEvent): AnalyticsEvent = DatePickerPayloadEvent(
        when (event) {
            DatePickerEvent.Submit -> Submit
            DatePickerEvent.Dismiss -> Dismiss
        }
    )
}
