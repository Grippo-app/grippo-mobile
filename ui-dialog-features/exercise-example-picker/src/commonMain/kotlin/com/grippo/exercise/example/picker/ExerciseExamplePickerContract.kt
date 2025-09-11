package com.grippo.exercise.example.picker

internal interface ExerciseExamplePickerContract {
    fun onExerciseExampleSelectClick(id: String)
    fun onMuscleGroupClick(id: String)
    fun onExerciseExampleDetailsClick(id: String)
    fun onQueryChange(value: String)
    fun onFiltersClick()
    fun onSortClick()
    fun onDismiss()

    companion object Empty : ExerciseExamplePickerContract {
        override fun onExerciseExampleSelectClick(id: String) {}
        override fun onMuscleGroupClick(id: String) {}
        override fun onExerciseExampleDetailsClick(id: String) {}
        override fun onQueryChange(value: String) {}
        override fun onFiltersClick() {}
        override fun onSortClick() {}
        override fun onDismiss() {}
    }
}