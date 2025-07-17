package com.grippo.exercise

internal interface ExerciseContract {
    fun dismiss()

    companion object Empty : ExerciseContract {
        override fun dismiss() {}
    }
}