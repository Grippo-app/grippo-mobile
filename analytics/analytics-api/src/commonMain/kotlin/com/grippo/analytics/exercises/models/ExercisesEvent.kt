package com.grippo.analytics.exercises.models

public sealed interface ExercisesEvent {
    public data class FilterClicked(val filter: LibraryFilter) : ExercisesEvent
    public data object ExerciseSelected : ExercisesEvent
    public data object LoadNextPage : ExercisesEvent
    public data object Back : ExercisesEvent
}
