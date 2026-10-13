package com.grippo.analytics.exercise.example.models

public sealed interface ExerciseExampleEvent {
    public data object DismissRequested : ExerciseExampleEvent
    public data object ActionClicked : ExerciseExampleEvent
    public data class TabSelected(val tab: ExerciseExampleTab) : ExerciseExampleEvent
}
