package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class AuthProcessRouter : BaseRouter {

    @Serializable
    public data object Login : AuthProcessRouter()

    @Serializable
    public data class Registration(val email: String?) : AuthProcessRouter()
}