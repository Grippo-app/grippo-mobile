package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable
    public data object Equipments : ProfileRouter()

    @Serializable
    public data object Muscles : ProfileRouter()

    @Serializable
    public data object Body : ProfileRouter()

    @Serializable
    public data object Experience : ProfileRouter()

    @Serializable
    public data object Settings : ProfileRouter()
}
