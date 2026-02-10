package com.grippo.confirm.training.completion

import com.grippo.core.foundation.models.BaseDirection

public sealed interface ConfirmTrainingCompletionDirection : BaseDirection {
    public data object Confirm : ConfirmTrainingCompletionDirection
    public data object Back : ConfirmTrainingCompletionDirection
}
