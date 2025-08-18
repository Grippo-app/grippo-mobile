package com.grippo.training

internal interface TrainingContract {
    fun onBack()
    fun toRecording()
    fun toExercise(id: String?)
    fun toSuccess()

    companion object Empty : TrainingContract {
        override fun onBack() {}
        override fun toRecording() {}
        override fun toExercise(id: String?) {}
        override fun toSuccess() {}
    }
}
