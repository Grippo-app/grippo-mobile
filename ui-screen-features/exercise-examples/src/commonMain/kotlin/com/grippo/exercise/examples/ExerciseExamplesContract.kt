package com.grippo.exercise.examples

internal interface ExerciseExamplesContract {
    fun onBack()

    companion object Empty : ExerciseExamplesContract {
        override fun onBack() {}
    }
}
