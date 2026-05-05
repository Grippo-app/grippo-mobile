package com.grippo.home.home

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.TrainingSeed

internal sealed interface HomeDirection : BaseDirection {
    data object Back : HomeDirection
    data class StartTraining(val seed: TrainingSeed) : HomeDirection
    data object Trainings : HomeDirection
    data object DraftTraining : HomeDirection

    data object ExcludedMuscles : HomeDirection
    data object MissingEquipment : HomeDirection
    data object Experience : HomeDirection
    data object Body : HomeDirection
    data object Goal : HomeDirection
    data object Debug : HomeDirection
    data object Settings : HomeDirection
    data object Social : HomeDirection
}
