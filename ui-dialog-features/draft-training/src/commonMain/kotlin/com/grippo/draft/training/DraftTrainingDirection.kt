package com.grippo.draft.training

import com.grippo.core.foundation.models.BaseDirection

public sealed interface DraftTrainingDirection : BaseDirection {
    public data object Continue : DraftTrainingDirection
    public data object Back : DraftTrainingDirection
}
