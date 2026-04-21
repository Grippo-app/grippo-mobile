package com.grippo.training.profile.details

import com.grippo.core.foundation.models.BaseDirection

public sealed interface TrainingProfileDetailsDirection : BaseDirection {
    public data object Back : TrainingProfileDetailsDirection
}
