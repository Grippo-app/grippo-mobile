package com.grippo.presentation.api

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class RootRouter : BaseRouter {
    public data object Auth : RootRouter()
}