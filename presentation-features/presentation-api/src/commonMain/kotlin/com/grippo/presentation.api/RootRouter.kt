package com.grippo.presentation.api

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable
    public data object Auth : RootRouter()

    @Serializable
    public data object Home : RootRouter()
}