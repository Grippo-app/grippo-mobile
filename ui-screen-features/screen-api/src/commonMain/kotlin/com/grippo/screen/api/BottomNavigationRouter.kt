package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class BottomNavigationRouter : BaseRouter {

    @Serializable
    public data object Statistics : BottomNavigationRouter()

    @Serializable
    public data object Trainings : BottomNavigationRouter()
}