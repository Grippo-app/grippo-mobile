package com.grippo.exercise.example.exerciseexample

internal interface ExerciseExampleContract {
    fun onDismiss()
    fun onSelectClick()

    companion object Empty : ExerciseExampleContract {
        override fun onDismiss() {}
        override fun onSelectClick() {}
    }
}