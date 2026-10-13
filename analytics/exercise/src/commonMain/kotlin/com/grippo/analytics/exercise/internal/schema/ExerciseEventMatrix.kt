package com.grippo.analytics.exercise.internal.schema

import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object ExerciseEventMatrix {
    private val ExampleDetailsClicked = AnalyticsEventSchema("exercise_example_details_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("exercise_dismiss_requested", 1, emptyList())

    val all = listOf(ExampleDetailsClicked, DismissRequested)

    fun event(event: ExerciseEvent): AnalyticsEvent = Payload(
        when (event) {
            ExerciseEvent.ExampleDetailsClicked -> ExampleDetailsClicked
            ExerciseEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
