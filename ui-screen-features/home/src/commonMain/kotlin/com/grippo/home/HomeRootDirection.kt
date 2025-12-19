package com.grippo.home

import com.grippo.core.foundation.models.BaseDirection

public sealed interface HomeRootDirection : BaseDirection {
    public data object Back : HomeRootDirection
    public data object ToExcludedMuscles : HomeRootDirection
    public data object ToMissingEquipment : HomeRootDirection
    public data object ToWeightHistory : HomeRootDirection
    public data object ToDebug : HomeRootDirection
    public data object ToAddTraining : HomeRootDirection
    public data object ToDraftTraining : HomeRootDirection
}
