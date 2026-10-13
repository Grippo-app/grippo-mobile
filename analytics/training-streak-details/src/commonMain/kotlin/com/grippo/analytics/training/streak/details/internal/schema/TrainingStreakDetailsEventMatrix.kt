package com.grippo.analytics.training.streak.details.internal.schema

import com.grippo.analytics.training.streak.details.models.TrainingStreakDetailsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object TrainingStreakDetailsEventMatrix {
    private val DismissRequested = AnalyticsEventSchema("training_streak_details_dismiss", 1, emptyList())

    val all = listOf(DismissRequested)

    fun event(event: TrainingStreakDetailsEvent): AnalyticsEvent = Payload(
        when (event) {
            TrainingStreakDetailsEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
