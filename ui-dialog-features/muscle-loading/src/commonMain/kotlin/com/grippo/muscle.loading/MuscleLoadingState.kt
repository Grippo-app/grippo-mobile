package com.grippo.muscle.loading

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.toolkit.date.utils.DateRange

@Immutable
public data class MuscleLoadingState(
    val range: DateRange,
    val summary: MuscleLoadSummaryState? = null,
)
