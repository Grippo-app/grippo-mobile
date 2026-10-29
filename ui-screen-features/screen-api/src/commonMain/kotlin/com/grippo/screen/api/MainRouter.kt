package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
public sealed class MainRouter : BaseRouter {
    @Serializable
    public data object Home : MainRouter()

    @Serializable
    public data class Calendar(
        val value: TrainingsRouter = TrainingsRouter.Trainings(),
        // Without a fresh identity, reopening an equal route reuses the stale calendar tab instance.
        @EncodeDefault val flowId: FlowId = FlowId.create(),
    ) : MainRouter()

    @Serializable
    public data object Library : MainRouter()

    @Serializable
    public data object Profile : MainRouter()
}
