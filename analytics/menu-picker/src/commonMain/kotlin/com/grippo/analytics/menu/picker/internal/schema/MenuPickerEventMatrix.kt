package com.grippo.analytics.menu.picker.internal.schema

import com.grippo.analytics.menu.picker.models.MenuPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object MenuPickerEventMatrix {
    private val ItemClicked = AnalyticsEventSchema("menu_picker_item_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("menu_picker_dismiss_requested", 1, emptyList())

    val all = listOf(ItemClicked, DismissRequested)

    fun event(event: MenuPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            MenuPickerEvent.ItemClicked -> ItemClicked
            MenuPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
