package com.grippo.exercise.example.picker

internal interface ExerciseExamplePickerContract {
    fun onExerciseExampleSelectClick(id: String)
    fun onSuggestClick()
    fun onClearSuggestion()
    fun onMuscleGroupClick(id: String)
    fun onQueryChange(value: String)
    fun onFiltersClick()
    fun onDismiss()

    companion object Empty : ExerciseExamplePickerContract {
        override fun onExerciseExampleSelectClick(id: String) {}
        override fun onMuscleGroupClick(id: String) {}
        override fun onQueryChange(value: String) {}
        override fun onClearSuggestion() {}
        override fun onFiltersClick() {}
        override fun onSuggestClick() {}
        override fun onDismiss() {}
    }
}