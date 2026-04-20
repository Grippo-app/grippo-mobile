package com.grippo.training.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.TrainingLoadProfileState as TrainingProfileSummary

@Immutable
public data class TrainingProfileDialogState(
    val range: DateRangeFormatState,
    val profile: TrainingProfileSummary? = null,
)
