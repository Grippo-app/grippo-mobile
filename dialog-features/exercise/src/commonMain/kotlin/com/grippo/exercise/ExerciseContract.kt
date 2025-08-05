package com.grippo.exercise

internal interface ExerciseContract {
    fun onDismiss()
    fun onExampleDetailsClick(id: String)

    companion object Empty : ExerciseContract {
        override fun onDismiss() {}
        override fun onExampleDetailsClick(id: String) {}
    }
}