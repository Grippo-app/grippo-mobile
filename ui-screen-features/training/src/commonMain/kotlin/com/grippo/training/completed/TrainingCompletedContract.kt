package com.grippo.training.completed

internal interface TrainingCompletedContract {
    fun onExerciseClick(id: String)
    fun onBack()

    companion object Empty : TrainingCompletedContract {
        override fun onExerciseClick(id: String) {}
        override fun onBack() {}
    }
}