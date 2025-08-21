package com.grippo.exercise.examples.list

internal interface ExerciseExampleListContract {
    fun onBack()
    fun onExerciseExampleClick(id: String)

    companion object Empty : ExerciseExampleListContract {
        override fun onBack() {}
        override fun onExerciseExampleClick(id: String) {}
    }
}
