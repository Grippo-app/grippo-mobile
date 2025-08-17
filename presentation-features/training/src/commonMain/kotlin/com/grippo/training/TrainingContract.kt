package com.grippo.training

internal interface TrainingContract {
    fun onBack()

    companion object Empty : TrainingContract {
        override fun onBack() {}
    }
}