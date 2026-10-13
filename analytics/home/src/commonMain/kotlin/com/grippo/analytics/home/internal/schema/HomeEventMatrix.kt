package com.grippo.analytics.home.internal.schema

import com.grippo.analytics.home.internal.events.HomePayloadEvent
import com.grippo.analytics.home.models.HomeEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema

internal object HomeEventMatrix {
    private val TrainingsClicked = AnalyticsEventSchema("home_trainings_clicked", 1, emptyList())
    private val ExerciseClicked = AnalyticsEventSchema("home_exercise_clicked", 1, emptyList())
    private val MuscleLoadingClicked = AnalyticsEventSchema("home_muscle_loading_clicked", 1, emptyList())
    private val TrainingStreakClicked = AnalyticsEventSchema("home_training_streak_clicked", 1, emptyList())
    private val GoalDetailsClicked = AnalyticsEventSchema("home_goal_details_clicked", 1, emptyList())
    private val PeriodPickerClicked = AnalyticsEventSchema("home_period_picker_clicked", 1, emptyList())
    private val PeriodConfirmed = AnalyticsEventSchema("home_period_confirmed", 1, emptyList())
    private val PerformanceMetricClicked = AnalyticsEventSchema("home_performance_metric_clicked", 1, emptyList())
    private val AddGoalClicked = AnalyticsEventSchema("home_add_goal_clicked", 1, emptyList())
    private val Back = AnalyticsEventSchema("home_back", 1, emptyList())

    val all = listOf(
        TrainingsClicked,
        ExerciseClicked,
        MuscleLoadingClicked,
        TrainingStreakClicked,
        GoalDetailsClicked,
        PeriodPickerClicked,
        PeriodConfirmed,
        PerformanceMetricClicked,
        AddGoalClicked,
        Back,
    )

    fun event(event: HomeEvent): AnalyticsEvent = HomePayloadEvent(
        when (event) {
            HomeEvent.TrainingsClicked -> TrainingsClicked
            HomeEvent.ExerciseClicked -> ExerciseClicked
            HomeEvent.MuscleLoadingClicked -> MuscleLoadingClicked
            HomeEvent.TrainingStreakClicked -> TrainingStreakClicked
            HomeEvent.GoalDetailsClicked -> GoalDetailsClicked
            HomeEvent.PeriodPickerClicked -> PeriodPickerClicked
            HomeEvent.PeriodConfirmed -> PeriodConfirmed
            HomeEvent.PerformanceMetricClicked -> PerformanceMetricClicked
            HomeEvent.AddGoalClicked -> AddGoalClicked
            HomeEvent.Back -> Back
        },
        emptyList(),
    )
}
