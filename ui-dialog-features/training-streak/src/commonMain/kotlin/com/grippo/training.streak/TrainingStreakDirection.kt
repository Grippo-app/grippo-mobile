package com.grippo.training.streak

import com.grippo.core.foundation.models.BaseDirection

public sealed interface TrainingStreakDirection : BaseDirection {
    public data object Back : TrainingStreakDirection
}
