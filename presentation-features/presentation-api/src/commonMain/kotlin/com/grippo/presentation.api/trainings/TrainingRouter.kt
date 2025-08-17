package com.grippo.presentation.api.trainings

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class TrainingRouter : BaseRouter {

    @Serializable
    public data object Preferences : TrainingRouter()

    @Serializable
    public data object Recording : TrainingRouter()

    @Serializable
    public data object Success : TrainingRouter()
}