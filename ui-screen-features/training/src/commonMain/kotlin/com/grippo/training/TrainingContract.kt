package com.grippo.training

import com.grippo.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

internal interface TrainingContract {
    fun onClose()
    fun onBack()
    fun toRecording(id: String?)
    fun toExercise(exercise: ExerciseState)
    fun toCompleted(exercises: List<ExerciseState>, startAt: LocalDateTime)

    companion object Empty : TrainingContract {
        override fun onClose() {}
        override fun onBack() {}
        override fun toRecording(id: String?) {}
        override fun toExercise(exercise: ExerciseState) {}
        override fun toCompleted(exercises: List<ExerciseState>, startAt: LocalDateTime) {}
    }
}
