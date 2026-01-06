package com.grippo.training.streak

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.core.state.metrics.TrainingStreakState as TrainingStreakDetails

@Immutable
public data class TrainingStreakDialogState(
    val range: DateRange,
    val streak: TrainingStreakDetails? = null,
)
