package com.grippo.analytics.exercise.models

public sealed interface ExerciseEvent {
    public data object ExampleDetailsClicked : ExerciseEvent
    public data object DismissRequested : ExerciseEvent
}
