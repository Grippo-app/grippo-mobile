package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class RegistrationRouter : BaseRouter {

    @Serializable
    public data object Credentials : RegistrationRouter()

    @Serializable
    public data object Name : RegistrationRouter()

    @Serializable
    public data object Body : RegistrationRouter()

    @Serializable
    public data object Experience : RegistrationRouter()

    @Serializable
    public data object ExcludedMuscles : RegistrationRouter()

    @Serializable
    public data object MissingEquipments : RegistrationRouter()

    @Serializable
    public data object Completed : RegistrationRouter()
}