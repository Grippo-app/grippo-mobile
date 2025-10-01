package com.grippo.training

import com.grippo.state.stage.StageState
import com.grippo.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

internal interface TrainingContract {
    fun onClose()
    fun onBack()
    fun toRecording(stage: StageState)
    fun toExercise(exercise: ExerciseState)
    fun toCompleted(stage: StageState, exercises: List<ExerciseState>, startAt: LocalDateTime)

    companion object Empty : TrainingContract {
        override fun onClose() {}
        override fun onBack() {}
        override fun toRecording(stage: StageState) {}
        override fun toExercise(exercise: ExerciseState) {}
        override fun toCompleted(
            stage: StageState,
            exercises: List<ExerciseState>,
            startAt: LocalDateTime
        ) {
        }
    }
}
