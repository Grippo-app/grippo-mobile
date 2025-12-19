package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class HomeRouter : BaseRouter {

    @Serializable
    public data object Home : HomeRouter()
}
