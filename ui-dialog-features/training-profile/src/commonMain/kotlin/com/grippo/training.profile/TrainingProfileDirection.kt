package com.grippo.training.profile

import com.grippo.core.foundation.models.BaseDirection

public sealed interface TrainingProfileDirection : BaseDirection {
    public data object Back : TrainingProfileDirection
}
