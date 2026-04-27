package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState

@Immutable
public data class TopMuscleContributionState(
    val muscle: MuscleEnumState,
    val share: Int,                  // % of period's total muscle work (0..100)
)
