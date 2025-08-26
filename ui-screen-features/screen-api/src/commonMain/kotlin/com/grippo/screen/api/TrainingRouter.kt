package com.grippo.screen.api

import com.grippo.core.models.BaseRouter
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState
import kotlinx.serialization.Serializable

@Serializable
public sealed class TrainingRouter : BaseRouter {

    @Serializable
    public data object Setup : TrainingRouter()

    @Serializable
    public data object Recording : TrainingRouter()

    @Serializable
    public data class Exercise(val exercise: ExerciseState) : TrainingRouter()

    @Serializable
    public data class Completed(val training: TrainingState) : TrainingRouter()
}