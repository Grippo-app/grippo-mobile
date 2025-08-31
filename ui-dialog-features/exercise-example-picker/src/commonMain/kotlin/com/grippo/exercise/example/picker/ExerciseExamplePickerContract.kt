package com.grippo.exercise.example.picker

internal interface ExerciseExamplePickerContract {
    fun onExerciseExampleSelectClick(id: String)
    fun onExerciseExampleDetailsClick(id: String)
    fun onQueryChange(value: String)
    fun onFiltersClick()
    fun onDismiss()

    companion object Empty : ExerciseExamplePickerContract {
        override fun onExerciseExampleSelectClick(id: String) {}
        override fun onExerciseExampleDetailsClick(id: String) {}
        override fun onQueryChange(value: String) {}
        override fun onFiltersClick() {}
        override fun onDismiss() {}
    }
}