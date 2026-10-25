package com.grippo.training

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import kotlinx.datetime.LocalDateTime

@Immutable
internal interface TrainingContract {
    fun toLibrary(route: ExercisesRouter.Library)
    fun onClose()
    fun onBack()
    fun toRecording(stage: StageState)
    fun toExercise(exercise: ExerciseState, resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>)
    fun toCompleted(stage: StageState, exercises: List<ExerciseState>, startAt: LocalDateTime)
    fun toGoal()

    @Immutable
    companion object Empty : TrainingContract {
        override fun toLibrary(route: ExercisesRouter.Library) {}
        override fun onClose() {}
        override fun onBack() {}
        override fun toRecording(stage: StageState) {}
        override fun toExercise(exercise: ExerciseState, resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>) {}
        override fun toCompleted(stage: StageState, exercises: List<ExerciseState>, startAt: LocalDateTime) {}
        override fun toGoal() {}
    }
}
