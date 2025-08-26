package com.grippo.training

import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

internal interface TrainingContract {
    fun onClose()
    fun onBack()
    fun toRecording()
    fun toExercise(exercise: ExerciseState)
    fun toCompleted(training: TrainingState)

    companion object Empty : TrainingContract {
        override fun onClose() {}
        override fun onBack() {}
        override fun toRecording() {}
        override fun toExercise(exercise: ExerciseState) {}
        override fun toCompleted(training: TrainingState) {}
    }
}
