package com.grippo.training

import com.grippo.core.models.BaseDirection

public sealed interface TrainingDirection : BaseDirection {
    public data object Back : TrainingDirection
}