package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class MainRouter : BaseRouter {
    @Serializable
    public data object Home : MainRouter()

    @Serializable
    public data object Calendar : MainRouter()

    @Serializable
    public data object Profile : MainRouter()
}
