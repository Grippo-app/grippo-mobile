package com.grippo.training

import androidx.compose.runtime.Immutable
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

@Immutable
internal interface TrainingContract {
    fun onClose()
    fun onBack()
    fun toRecording(stage: StageState)
    fun toExercise(exercise: ExerciseState)
    fun toCompleted(stage: StageState, exercises: List<ExerciseState>, startAt: LocalDateTime)

    @Immutable
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
