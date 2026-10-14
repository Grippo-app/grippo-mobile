package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
public sealed class AuthRouter : BaseRouter {

    @Serializable
    public data object Splash : AuthRouter()

    @JsonIgnoreUnknownKeys
    @Serializable
    public data object Onboarding : AuthRouter()

    // Older saved routes may contain obsolete entry attribution.
    @JsonIgnoreUnknownKeys
    @Serializable
    public data class AuthProcess(
        // Route identity prevents reusing an exited AuthProcess for a new scenario.
        @EncodeDefault val flowId: FlowId = FlowId.create(),
    ) : AuthRouter()

    @Serializable
    public data object ProfileCreation : AuthRouter()
}
