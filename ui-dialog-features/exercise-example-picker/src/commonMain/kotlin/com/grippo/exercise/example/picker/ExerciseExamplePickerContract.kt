package com.grippo.exercise.example.picker

import com.grippo.state.datetime.PeriodState

internal interface ExerciseExamplePickerContract {
    fun onExerciseExampleSelectClick(value: PeriodState)
    fun onExerciseExampleDetailsClick(id: String)
    fun onQueryChange(value: String)
    fun onFiltersClick()
    fun onDismiss()

    companion object Empty : ExerciseExamplePickerContract {
        override fun onExerciseExampleSelectClick(value: PeriodState) {}
        override fun onExerciseExampleDetailsClick(id: String) {}
        override fun onQueryChange(value: String) {}
        override fun onFiltersClick() {}
        override fun onDismiss() {}
    }
}