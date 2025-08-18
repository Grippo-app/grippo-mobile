package com.grippo.training

internal interface TrainingContract {
    fun onBack()
    fun toRecording()
    fun toSuccess()

    companion object Empty : TrainingContract {
        override fun onBack() {}
        override fun toRecording() {}
        override fun toSuccess() {}
    }
}
