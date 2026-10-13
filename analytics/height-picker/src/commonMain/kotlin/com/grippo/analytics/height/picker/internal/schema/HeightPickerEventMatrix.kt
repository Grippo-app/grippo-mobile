package com.grippo.analytics.height.picker.internal.schema

import com.grippo.analytics.height.picker.models.HeightPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object HeightPickerEventMatrix {
    private val SubmitClicked = AnalyticsEventSchema("height_picker_submit_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("height_picker_dismiss_requested", 1, emptyList())

    val all = listOf(SubmitClicked, DismissRequested)

    fun event(event: HeightPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            HeightPickerEvent.SubmitClicked -> SubmitClicked
            HeightPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
