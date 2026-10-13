package com.grippo.analytics.confirmation.internal.schema

import com.grippo.analytics.confirmation.models.ConfirmationEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object ConfirmationEventMatrix {
    private val ConfirmClicked = AnalyticsEventSchema("confirmation_confirm", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("confirmation_dismiss_requested", 1, emptyList())

    val all = listOf(ConfirmClicked, DismissRequested)

    fun event(event: ConfirmationEvent): AnalyticsEvent = Payload(
        when (event) {
            ConfirmationEvent.ConfirmClicked -> ConfirmClicked
            ConfirmationEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
