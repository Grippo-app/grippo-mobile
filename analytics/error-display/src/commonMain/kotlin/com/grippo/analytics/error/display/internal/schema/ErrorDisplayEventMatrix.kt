package com.grippo.analytics.error.display.internal.schema

import com.grippo.analytics.error.display.models.ErrorDisplayEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object ErrorDisplayEventMatrix {
    private val RetryRequested = AnalyticsEventSchema("error_display_retry_requested", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("error_display_dismiss_requested", 1, emptyList())

    val all = listOf(RetryRequested, DismissRequested)

    fun event(event: ErrorDisplayEvent): AnalyticsEvent = Payload(
        when (event) {
            ErrorDisplayEvent.RetryRequested -> RetryRequested
            ErrorDisplayEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
