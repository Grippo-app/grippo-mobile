package com.grippo.presentation.api.auth

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class AuthProcessRouter : BaseRouter {

    @Serializable
    public data object Login : AuthProcessRouter()

    @Serializable
    public data object Registration : AuthProcessRouter()
}