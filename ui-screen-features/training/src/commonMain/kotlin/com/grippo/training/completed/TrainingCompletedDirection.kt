package com.grippo.training.completed

import com.grippo.core.models.BaseDirection

internal sealed interface TrainingCompletedDirection : BaseDirection {
    data object Back : TrainingCompletedDirection
}