package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class AuthRouter : BaseRouter {

    @Serializable
    public data object Splash : AuthRouter()

    @Serializable
    public data object AuthProcess : AuthRouter()
}