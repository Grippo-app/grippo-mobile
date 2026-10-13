package com.grippo.analytics.weight.picker.internal.schema

import com.grippo.analytics.weight.picker.models.WeightPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object WeightPickerEventMatrix {
    private val SelectionChanged = AnalyticsEventSchema("weight_picker_weight_selected", 1, emptyList())
    private val SubmitClicked = AnalyticsEventSchema("weight_picker_submit_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("weight_picker_dismiss_requested", 1, emptyList())

    val all = listOf(SelectionChanged, SubmitClicked, DismissRequested)

    fun event(event: WeightPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            WeightPickerEvent.SelectionChanged -> SelectionChanged
            WeightPickerEvent.SubmitClicked -> SubmitClicked
            WeightPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
