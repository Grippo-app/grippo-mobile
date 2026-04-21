package com.grippo.muscle.loading.details

import com.grippo.core.foundation.models.BaseDirection

public sealed interface MuscleLoadingDetailsDirection : BaseDirection {
    public data object Back : MuscleLoadingDetailsDirection
}
