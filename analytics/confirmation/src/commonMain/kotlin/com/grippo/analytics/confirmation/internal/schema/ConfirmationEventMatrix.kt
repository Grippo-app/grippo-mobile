package com.grippo.analytics.confirmation.internal.schema

import com.grippo.analytics.confirmation.models.ConfirmationEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object ConfirmationEventMatrix {
    private val Confirm = AnalyticsEventSchema("confirmation_confirm", 1, emptyList())
    private val Back = AnalyticsEventSchema("confirmation_back", 1, emptyList())

    val all = listOf(Confirm, Back)

    fun event(event: ConfirmationEvent): AnalyticsEvent = Payload(
        when (event) {
            ConfirmationEvent.Confirm -> Confirm
            ConfirmationEvent.Back -> Back
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
