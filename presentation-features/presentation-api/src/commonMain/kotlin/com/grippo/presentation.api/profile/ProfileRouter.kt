package com.grippo.presentation.api.profile

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable
    public data object Equipments : ProfileRouter()

    @Serializable
    public data object Muscles : ProfileRouter()
}