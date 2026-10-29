package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
public sealed class TrainingsRouter : BaseRouter {

    @Serializable
    public data class Trainings(
        val day: LocalDate? = null,
    ) : TrainingsRouter()
}
