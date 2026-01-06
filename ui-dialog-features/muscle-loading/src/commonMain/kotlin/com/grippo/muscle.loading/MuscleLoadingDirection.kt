package com.grippo.muscle.loading

import com.grippo.core.foundation.models.BaseDirection

public sealed interface MuscleLoadingDirection : BaseDirection {
    public data object Back : MuscleLoadingDirection
}
