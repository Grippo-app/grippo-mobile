package com.grippo.training

import com.grippo.state.trainings.ExerciseState


internal interface TrainingContract {
    fun onClose()
    fun onBack()
    fun toRecording()
    fun toExercise(exercise: ExerciseState)
    fun toSuccess()

    companion object Empty : TrainingContract {
        override fun onClose() {}
        override fun onBack() {}
        override fun toRecording() {}
        override fun toExercise(exercise: ExerciseState) {}
        override fun toSuccess() {}
    }
}
