package com.grippo.analytics.exercise.example.internal.schema

import com.grippo.analytics.exercise.example.internal.events.ExerciseExamplePayloadEvent
import com.grippo.analytics.exercise.example.models.ExerciseExampleEvent
import com.grippo.analytics.exercise.example.models.ExerciseExampleTab
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object ExerciseExampleEventMatrix {
    private val Tab = AnalyticsParameterKey.code<ExerciseExampleTab>("exercise_example_tab") {
        when (it) {
            ExerciseExampleTab.Information -> "information"
            ExerciseExampleTab.Achievements -> "achievements"
        }
    }
    private val Dismiss = AnalyticsEventSchema("exercise_example_dismiss_requested", 1, emptyList())
    private val Action = AnalyticsEventSchema("exercise_example_action_clicked", 1, emptyList())
    private val SelectTab = AnalyticsEventSchema("exercise_example_tab_selected", 1, listOf(Tab))

    val all = listOf(Dismiss, Action, SelectTab)

    fun event(event: ExerciseExampleEvent): AnalyticsEvent = when (event) {
        ExerciseExampleEvent.DismissRequested -> ExerciseExamplePayloadEvent(Dismiss, emptyList())
        ExerciseExampleEvent.ActionClicked -> ExerciseExamplePayloadEvent(Action, emptyList())
        is ExerciseExampleEvent.TabSelected -> ExerciseExamplePayloadEvent(SelectTab, listOf(Tab.value(event.tab)))
    }
}
