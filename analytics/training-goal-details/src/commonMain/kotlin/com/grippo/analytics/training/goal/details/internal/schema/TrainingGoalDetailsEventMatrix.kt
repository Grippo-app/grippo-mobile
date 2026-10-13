package com.grippo.analytics.training.goal.details.internal.schema

import com.grippo.analytics.training.goal.details.models.TrainingGoalDetailsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object TrainingGoalDetailsEventMatrix {
    private val DismissRequested = AnalyticsEventSchema("training_goal_details_dismiss_requested", 1, emptyList())

    private val AddGoalClicked = AnalyticsEventSchema("training_goal_details_add_goal", 1, emptyList())

    val all = listOf(DismissRequested, AddGoalClicked)

    fun event(event: TrainingGoalDetailsEvent): AnalyticsEvent = Payload(
        when (event) {
            TrainingGoalDetailsEvent.DismissRequested -> DismissRequested
            TrainingGoalDetailsEvent.AddGoalClicked -> AddGoalClicked
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
