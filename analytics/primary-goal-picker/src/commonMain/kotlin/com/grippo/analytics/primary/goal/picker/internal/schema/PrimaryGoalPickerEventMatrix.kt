package com.grippo.analytics.primary.goal.picker.internal.schema

import com.grippo.analytics.primary.goal.picker.models.PrimaryGoalPickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object PrimaryGoalPickerEventMatrix {
    private val GoalSelected = AnalyticsEventSchema("primary_goal_picker_goal_selected", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("primary_goal_picker_dismiss_requested", 1, emptyList())

    val all = listOf(GoalSelected, DismissRequested)

    fun event(event: PrimaryGoalPickerEvent): AnalyticsEvent = Payload(
        when (event) {
            PrimaryGoalPickerEvent.GoalSelected -> GoalSelected
            PrimaryGoalPickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
