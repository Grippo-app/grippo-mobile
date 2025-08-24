package com.grippo.training.success

internal interface TrainingSuccessContract {
    fun onBack()

    companion object Empty : TrainingSuccessContract {
        override fun onBack() {}
    }
}