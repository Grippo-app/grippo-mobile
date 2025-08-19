package com.grippo.training

internal interface TrainingContract {
    fun onClose()
    fun onBack()
    fun toRecording()
    fun toExercise(id: String?)
    fun toSuccess()

    companion object Empty : TrainingContract {
        override fun onClose() {}
        override fun onBack() {}
        override fun toRecording() {}
        override fun toExercise(id: String?) {}
        override fun toSuccess() {}
    }
}
