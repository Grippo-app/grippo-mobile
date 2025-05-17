package com.grippo.presentation.api.bottom.navigation

import com.grippo.core.models.BaseRouter
import kotlinx.serialization.Serializable

@Serializable
public sealed class BottomNavigationRouter : BaseRouter {

    @Serializable
    public data object Profile : BottomNavigationRouter()

    @Serializable
    public data object Trainings : BottomNavigationRouter()
}