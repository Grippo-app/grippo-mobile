package com.grippo.exercise

internal interface ExerciseContract {
    fun dismiss()
    fun onExampleDetailsClick(id: String)

    companion object Empty : ExerciseContract {
        override fun dismiss() {}
        override fun onExampleDetailsClick(id: String) {}
    }
}