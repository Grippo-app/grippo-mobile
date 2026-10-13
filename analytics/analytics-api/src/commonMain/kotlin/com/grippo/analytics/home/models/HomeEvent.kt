package com.grippo.analytics.home.models

public sealed interface HomeEvent {
    public data object TrainingsClicked : HomeEvent
    public data object ExerciseClicked : HomeEvent
    public data object MuscleLoadingClicked : HomeEvent
    public data object TrainingStreakClicked : HomeEvent
    public data object GoalDetailsClicked : HomeEvent
    public data object PeriodPickerClicked : HomeEvent
    public data object PeriodConfirmed : HomeEvent
    public data object PerformanceMetricClicked : HomeEvent
    public data object AddGoalClicked : HomeEvent
    public data object Back : HomeEvent
}
