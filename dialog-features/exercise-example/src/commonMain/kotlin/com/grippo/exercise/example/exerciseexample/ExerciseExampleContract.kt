package com.grippo.exercise.example.exerciseexample

internal interface ExerciseExampleContract {
    fun dismiss()

    companion object Empty : ExerciseExampleContract {
        override fun dismiss() {}
    }
}