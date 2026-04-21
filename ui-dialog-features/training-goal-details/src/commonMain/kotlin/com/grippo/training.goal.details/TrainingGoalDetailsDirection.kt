package com.grippo.training.goal.details

import com.grippo.core.foundation.models.BaseDirection

public sealed interface TrainingGoalDetailsDirection : BaseDirection {
    public data object Back : TrainingGoalDetailsDirection
}
