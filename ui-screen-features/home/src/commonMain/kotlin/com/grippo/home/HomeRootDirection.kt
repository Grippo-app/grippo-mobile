package com.grippo.home

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.screen.api.TrainingsRouter

public sealed interface HomeRootDirection : BaseDirection {
    public data object Back : HomeRootDirection
    public data class Trainings(val route: TrainingsRouter) : HomeRootDirection
    public data object Goal : HomeRootDirection
}
