package com.grippo.training.streak.details

import com.grippo.core.foundation.models.BaseDirection

public sealed interface TrainingStreakDetailsDirection : BaseDirection {
    public data object Back : TrainingStreakDetailsDirection
}
