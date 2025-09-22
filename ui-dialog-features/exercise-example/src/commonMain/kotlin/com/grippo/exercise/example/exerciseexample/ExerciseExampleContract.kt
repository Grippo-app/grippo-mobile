package com.grippo.exercise.example.exerciseexample

internal interface ExerciseExampleContract {
    fun onDismiss()

    companion object Empty : ExerciseExampleContract {
        override fun onDismiss() {}
    }
}