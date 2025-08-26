package com.grippo.training.completed

internal interface TrainingCompletedContract {
    fun onBack()

    companion object Empty : TrainingCompletedContract {
        override fun onBack() {}
    }
}