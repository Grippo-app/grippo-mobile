package com.grippo.training.profile

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.core.state.metrics.TrainingLoadProfileState as TrainingProfileSummary

@Immutable
public data class TrainingProfileDialogState(
    val range: DateRange,
    val profile: TrainingProfileSummary? = null,
)
