package com.grippo.analytics.exercise.example.internal.events

import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal class ExerciseExamplePayloadEvent(
    override val schema: AnalyticsEventSchema,
    private val parameters: List<AnalyticsParameter>,
) : AnalyticsEvent {
    override fun parameters(): List<AnalyticsParameter> = parameters
}
