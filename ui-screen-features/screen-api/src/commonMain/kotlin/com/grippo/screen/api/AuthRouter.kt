package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
public sealed class AuthRouter : BaseRouter {

    @Serializable
    public data object Splash : AuthRouter()

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    public data class AuthProcess(
        val entryPoint: AuthorizationEntryPoint = AuthorizationEntryPoint.RestoredScenario,
        // Route identity prevents reusing an exited AuthProcess for a new scenario.
        @EncodeDefault val flowId: FlowId = FlowId.create(),
    ) : AuthRouter()

    @Serializable
    public data object ProfileCreation : AuthRouter()
}
