package com.grippo.training.success

import com.grippo.core.models.BaseDirection

public sealed interface TrainingSuccessDirection : BaseDirection {
    public data object Back : TrainingSuccessDirection
}