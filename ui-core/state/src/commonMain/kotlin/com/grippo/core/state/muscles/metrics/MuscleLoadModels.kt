package com.grippo.core.state.muscles.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class MuscleLoadSummary(
    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,
)

@Immutable
public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

@Immutable
public data class MuscleLoadEntry(
    val label: String,
    val value: Float,
    val muscles: ImmutableList<MuscleEnumState>,
)