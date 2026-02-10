package com.grippo.confirm.training.completion

import com.grippo.core.foundation.models.BaseDirection
import kotlin.time.Duration

public sealed interface ConfirmTrainingCompletionDirection : BaseDirection {
    public data class Confirm(val value: Duration) : ConfirmTrainingCompletionDirection
    public data object Back : ConfirmTrainingCompletionDirection
}
