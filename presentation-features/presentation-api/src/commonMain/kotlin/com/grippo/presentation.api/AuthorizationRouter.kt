package com.grippo.presentation.api

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class AuthRouter : BaseRouter {
    @Serializable
    public data object Splash : AuthRouter()

    @Serializable
    public data class Auth(
        val authSubRouter: AuthSubRouter,
    ) : AuthRouter()
}

@Serializable
public sealed class AuthSubRouter : BaseRouter {

    @Serializable
    public data object Login : AuthSubRouter()

    @Serializable
    public data object Registration : AuthSubRouter()
}