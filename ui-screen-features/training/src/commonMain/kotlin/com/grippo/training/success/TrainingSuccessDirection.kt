package com.grippo.training.success

import com.grippo.core.models.BaseDirection

internal sealed interface TrainingSuccessDirection : BaseDirection {
    data object Back : TrainingSuccessDirection
}