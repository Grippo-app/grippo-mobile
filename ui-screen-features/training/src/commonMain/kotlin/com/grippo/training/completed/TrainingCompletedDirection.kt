package com.grippo.training.completed

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface TrainingCompletedDirection : BaseDirection {
    data object Back : TrainingCompletedDirection
}