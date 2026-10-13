package com.grippo.analytics.secondary.goal.picker.internal.schema

import com.grippo.analytics.secondary.goal.picker.models.SecondaryGoalPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object SecondaryGoalPickerEventMatrix {
    private val GoalClicked = AnalyticsEventSchema("secondary_goal_picker_goal_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("secondary_goal_picker_dismiss_requested", 1, emptyList())

    val all = listOf(GoalClicked, DismissRequested)

    fun event(event: SecondaryGoalPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            SecondaryGoalPickerEvent.GoalClicked -> GoalClicked
            SecondaryGoalPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
