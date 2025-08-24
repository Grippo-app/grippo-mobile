package com.grippo.screen.api

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class ExerciseExamplesRouter : BaseRouter {

    @Serializable
    public data object List : ExerciseExamplesRouter()
}