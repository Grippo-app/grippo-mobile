package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState

@Immutable
public data class TrainingStreakProgressState(
    val progressPercent: Int,
    val achievedSessions: Int,
    val targetSessions: Int,
    val range: DateRangeFormatState,
)
