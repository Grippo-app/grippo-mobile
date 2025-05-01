package com.grippo.presentation.api

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class AuthRouter : BaseRouter {
    @Serializable
    public data object Splash : AuthRouter()

    @Serializable
    public data object AuthProcess : AuthRouter()
}

@Serializable
public sealed class AuthProcessRouter : BaseRouter {

    @Serializable
    public data object Login : AuthProcessRouter()

    @Serializable
    public data object Registration : AuthProcessRouter()
}