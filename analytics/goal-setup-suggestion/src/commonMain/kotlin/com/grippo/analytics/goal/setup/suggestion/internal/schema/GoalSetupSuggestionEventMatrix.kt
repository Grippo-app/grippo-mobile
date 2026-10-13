package com.grippo.analytics.goal.setup.suggestion.internal.schema

import com.grippo.analytics.goal.setup.suggestion.models.GoalSetupSuggestionEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object GoalSetupSuggestionEventMatrix {
    private val Configure = AnalyticsEventSchema("goal_setup_suggestion_configure", 1, emptyList())
    private val Later = AnalyticsEventSchema("goal_setup_suggestion_later", 1, emptyList())
    private val Back = AnalyticsEventSchema("goal_setup_suggestion_back", 1, emptyList())

    val all = listOf(Configure, Later, Back)

    fun event(event: GoalSetupSuggestionEvent): AnalyticsEvent = Payload(
        when (event) {
            GoalSetupSuggestionEvent.Configure -> Configure
            GoalSetupSuggestionEvent.Later -> Later
            GoalSetupSuggestionEvent.Back -> Back
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
