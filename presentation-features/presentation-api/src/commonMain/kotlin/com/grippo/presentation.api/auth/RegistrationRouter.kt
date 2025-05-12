package com.grippo.presentation.api.auth

import com.grippo.core.models.BaseRouter
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
    public data object MissingEquipment : RegistrationRouter()

    @Serializable
    public data class Completed(val name: String) : RegistrationRouter()
}