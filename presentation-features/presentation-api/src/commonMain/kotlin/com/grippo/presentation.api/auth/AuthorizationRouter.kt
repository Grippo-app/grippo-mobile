package com.grippo.presentation.api.auth

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class AuthRouter : BaseRouter {

    @Serializable
    public data object Splash : AuthRouter()

    @Serializable
    public data object AuthProcess : AuthRouter()
}