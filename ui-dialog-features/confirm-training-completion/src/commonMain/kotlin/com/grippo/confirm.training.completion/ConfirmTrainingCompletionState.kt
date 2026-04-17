package com.grippo.confirm.training.completion

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DurationFormatState

@Immutable
public data class ConfirmTrainingCompletionState(
    val duration: DurationFormatState,
)
