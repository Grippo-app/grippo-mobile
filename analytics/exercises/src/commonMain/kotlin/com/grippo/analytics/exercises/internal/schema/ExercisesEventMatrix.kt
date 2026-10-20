package com.grippo.analytics.exercises.internal.schema

import com.grippo.analytics.exercises.models.ExercisesEvent
import com.grippo.analytics.exercises.models.LibraryFilter
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object ExercisesEventMatrix {
    private val Filter = AnalyticsParameterKey.code<LibraryFilter>("library_filter") {
        when (it) {
            LibraryFilter.Suggestions -> "suggestions"
            LibraryFilter.MuscleGroup -> "muscle_group"
        }
    }
    private val FilterClicked = AnalyticsEventSchema("exercises_filter_clicked", 1, listOf(Filter))
    private val ExerciseSelected = AnalyticsEventSchema("exercises_exercise_selected", 1, emptyList())
    private val ExerciseDetailsClicked =
        AnalyticsEventSchema("exercises_exercise_details_clicked", 1, emptyList())
    private val LoadNextPage = AnalyticsEventSchema("exercises_load_next_page", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("exercises_dismiss_requested", 1, emptyList())

    val all = listOf(FilterClicked, ExerciseSelected, ExerciseDetailsClicked, LoadNextPage, DismissRequested)

    fun event(event: ExercisesEvent): AnalyticsEvent = when (event) {
        is ExercisesEvent.FilterClicked -> Payload(FilterClicked, listOf(Filter.value(event.filter)))
        ExercisesEvent.ExerciseSelected -> Payload(ExerciseSelected, emptyList())
        ExercisesEvent.ExerciseDetailsClicked -> Payload(ExerciseDetailsClicked, emptyList())
        ExercisesEvent.LoadNextPage -> Payload(LoadNextPage, emptyList())
        ExercisesEvent.DismissRequested -> Payload(DismissRequested, emptyList())
    }

    private class Payload(
        override val schema: AnalyticsEventSchema,
        private val values: List<AnalyticsParameter>,
    ) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = values
    }
}
