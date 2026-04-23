package com.grippo.training.profile.details

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.profile.TrainingLoadProfileState as TrainingProfileSummary

@Immutable
public data class TrainingProfileDetailsDialogState(
    val range: DateRangeFormatState,
    val profile: TrainingProfileSummary? = null,
)
