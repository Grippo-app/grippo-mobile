package com.grippo.home.home

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.screen.api.TrainingsRouter

internal sealed interface HomeDirection : BaseDirection {
    data object Back : HomeDirection
    data class Trainings(val route: TrainingsRouter) : HomeDirection
    data object Goal : HomeDirection
}
