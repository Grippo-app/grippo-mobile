package com.grippo.exercise.example.picker

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseExamplePickerContract {
    fun onExerciseExampleSelectClick(id: String)
    fun onMuscleGroupClick(id: String)
    fun onQueryChange(value: String)
    fun onLoadNextPage()
    fun onDismiss()

    @Immutable
    companion object Empty : ExerciseExamplePickerContract {
        override fun onExerciseExampleSelectClick(id: String) {}
        override fun onMuscleGroupClick(id: String) {}
        override fun onQueryChange(value: String) {}
        override fun onLoadNextPage() {}
        override fun onDismiss() {}
    }
}
