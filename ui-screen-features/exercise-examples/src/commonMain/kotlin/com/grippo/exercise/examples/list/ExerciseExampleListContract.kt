package com.grippo.exercise.examples.list

internal interface ExerciseExampleListContract {
    fun onBack()
    fun onExerciseExampleClick(id: String)
    fun onQueryChange(value: String)

    companion object Empty : ExerciseExampleListContract {
        override fun onBack() {}
        override fun onExerciseExampleClick(id: String) {}
        override fun onQueryChange(value: String) {}
    }
}
