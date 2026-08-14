package com.grippo.main

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
internal sealed class MainRouter : BaseRouter {

    @Serializable
    internal data object Home : MainRouter()

    @Serializable
    internal data object Calendar : MainRouter()

    @Serializable
    internal data object Exercises : MainRouter()

    @Serializable
    internal data object Profile : MainRouter()
}
