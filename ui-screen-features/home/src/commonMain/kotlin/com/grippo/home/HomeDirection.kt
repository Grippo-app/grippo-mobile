package com.grippo.home

import com.grippo.core.foundation.models.BaseDirection

public sealed interface HomeDirection : BaseDirection {
    public data object Back : HomeDirection
    public data object ToExcludedMuscles : HomeDirection
    public data object ToMissingEquipment : HomeDirection
    public data object ToWeightHistory : HomeDirection
    public data object ToDebug : HomeDirection
    public data class ToEditTraining(val id: String) : HomeDirection
    public data object ToAddTraining : HomeDirection
    public data object ToDraftTraining : HomeDirection
}