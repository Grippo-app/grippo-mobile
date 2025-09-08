package com.grippo.drart.training

import com.grippo.core.models.BaseDirection

public sealed interface DraftTrainingDirection : BaseDirection {
    public data object Continue : DraftTrainingDirection
    public data object Back : DraftTrainingDirection
}
