package com.grippo.analytics.duration.picker.internal.schema

import com.grippo.analytics.duration.picker.models.DurationPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object DurationPickerEventMatrix {
    private val SubmitClicked = AnalyticsEventSchema("duration_picker_submit_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("duration_picker_dismiss_requested", 1, emptyList())

    val all = listOf(SubmitClicked, DismissRequested)

    fun event(event: DurationPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            DurationPickerEvent.SubmitClicked -> SubmitClicked
            DurationPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
