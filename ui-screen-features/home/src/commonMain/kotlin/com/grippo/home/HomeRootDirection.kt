package com.grippo.home

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.TrainingSeed

public sealed interface HomeRootDirection : BaseDirection {
    public data object Back : HomeRootDirection
    public data object ExcludedMuscles : HomeRootDirection
    public data object MissingEquipment : HomeRootDirection
    public data object Body : HomeRootDirection
    public data object Experience : HomeRootDirection
    public data object Debug : HomeRootDirection
    public data object Trainings : HomeRootDirection
    public data class StartTraining(val seed: TrainingSeed) : HomeRootDirection
    public data object DraftTraining : HomeRootDirection
    public data object Settings : HomeRootDirection
    public data object Social : HomeRootDirection
    public data object Goal : HomeRootDirection
}
