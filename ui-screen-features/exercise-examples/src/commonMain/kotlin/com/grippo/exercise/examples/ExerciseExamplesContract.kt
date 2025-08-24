package com.grippo.exercise.examples

internal interface ExerciseExamplesContract {
    fun onBack()
    fun onClose()

    companion object Empty : ExerciseExamplesContract {
        override fun onBack() {}
        override fun onClose() {}
    }
}
