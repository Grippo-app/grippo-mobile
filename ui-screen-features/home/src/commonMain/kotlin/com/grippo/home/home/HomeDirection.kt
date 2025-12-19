package com.grippo.home.home

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface HomeDirection : BaseDirection {
    data object Back : HomeDirection
    data object AddTraining : HomeDirection

    data object ExcludedMuscles : HomeDirection
    data object MissingEquipment : HomeDirection
    data object WeightHistory : HomeDirection
    data object Debug : HomeDirection
}