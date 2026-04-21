package com.grippo.training.streak.details

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.TrainingStreakState as TrainingStreakDetails

@Immutable
public data class TrainingStreakDetailsDialogState(
    val range: DateRangeFormatState,
    val streak: TrainingStreakDetails? = null,
)
