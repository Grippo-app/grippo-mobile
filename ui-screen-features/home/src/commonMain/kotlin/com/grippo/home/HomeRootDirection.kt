package com.grippo.home

import com.grippo.core.foundation.models.BaseDirection

public sealed interface HomeRootDirection : BaseDirection {
    public data object Back : HomeRootDirection
    public data object ExcludedMuscles : HomeRootDirection
    public data object MissingEquipment : HomeRootDirection
    public data object WeightAndHeight : HomeRootDirection
    public data object Experience : HomeRootDirection
    public data object Debug : HomeRootDirection
    public data object Trainings : HomeRootDirection
    public data object AddTraining : HomeRootDirection
    public data object DraftTraining : HomeRootDirection
    public data object Settings : HomeRootDirection
}
