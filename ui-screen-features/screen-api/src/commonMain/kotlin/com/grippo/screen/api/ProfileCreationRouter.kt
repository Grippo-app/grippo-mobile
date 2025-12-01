package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class ProfileCreationRouter : BaseRouter {

    @Serializable
    public data object Name : ProfileCreationRouter()

    @Serializable
    public data object Body : ProfileCreationRouter()

    @Serializable
    public data object Experience : ProfileCreationRouter()

    @Serializable
    public data object ExcludedMuscles : ProfileCreationRouter()

    @Serializable
    public data object MissingEquipments : ProfileCreationRouter()

    @Serializable
    public data object Completed : ProfileCreationRouter()
}
