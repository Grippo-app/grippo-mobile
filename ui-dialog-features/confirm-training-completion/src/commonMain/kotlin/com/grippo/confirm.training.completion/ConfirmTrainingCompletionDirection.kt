package com.grippo.confirm.training.completion

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.formatters.DurationFormatState

public sealed interface ConfirmTrainingCompletionDirection : BaseDirection {
    public data class Confirm(val value: DurationFormatState) : ConfirmTrainingCompletionDirection
    public data object Back : ConfirmTrainingCompletionDirection
}
