package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleGroupEnumState

@Immutable
public data class TopMuscleGroupContributionState(
    val group: MuscleGroupEnumState,
    val share: Int,                  // % of period's total muscle work (0..100)
)
