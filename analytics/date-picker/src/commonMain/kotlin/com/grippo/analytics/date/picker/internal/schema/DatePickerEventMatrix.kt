package com.grippo.analytics.date.picker.internal.schema

import com.grippo.analytics.date.picker.internal.events.DatePickerPayloadEvent
import com.grippo.analytics.date.picker.models.DatePickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema

internal object DatePickerEventMatrix {
    private val SubmitClicked = AnalyticsEventSchema("date_picker_submit", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("date_picker_dismiss_requested", 1, emptyList())

    val all: List<AnalyticsEventSchema> = listOf(SubmitClicked, DismissRequested)

    fun event(event: DatePickerEvent): AnalyticsEvent = DatePickerPayloadEvent(
        when (event) {
            DatePickerEvent.SubmitClicked -> SubmitClicked
            DatePickerEvent.DismissRequested -> DismissRequested
        }
    )
}
