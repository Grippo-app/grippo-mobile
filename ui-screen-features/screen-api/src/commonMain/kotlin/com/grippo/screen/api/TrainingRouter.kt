package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
public sealed class TrainingRouter : BaseRouter {

    @Serializable
    public data class Recording(val stage: StageState) : TrainingRouter()

    @Serializable
    public data class Exercise(val exercise: ExerciseState) : TrainingRouter()

    @Serializable
    public data class Completed(
        val stage: StageState,
        val exercises: List<ExerciseState>,
        val startAt: LocalDateTime,
    ) : TrainingRouter()
}