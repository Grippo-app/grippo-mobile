package com.grippo.analytics.exercise.internal.schema

import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object ExerciseEventMatrix {
    private val ExampleDetailsClicked = AnalyticsEventSchema("exercise_example_details_clicked", 1, emptyList())
    private val Dismiss = AnalyticsEventSchema("exercise_dismiss", 1, emptyList())

    val all = listOf(ExampleDetailsClicked, Dismiss)

    fun event(event: ExerciseEvent): AnalyticsEvent = Payload(
        when (event) {
            ExerciseEvent.ExampleDetailsClicked -> ExampleDetailsClicked
            ExerciseEvent.Dismiss -> Dismiss
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
