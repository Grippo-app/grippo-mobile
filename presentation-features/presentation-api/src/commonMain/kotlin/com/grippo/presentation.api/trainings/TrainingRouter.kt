package com.grippo.presentation.api.trainings

import com.grippo.core.models.BaseRouter
import com.grippo.state.trainings.ExerciseState
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
    public data object Success : TrainingRouter()
}