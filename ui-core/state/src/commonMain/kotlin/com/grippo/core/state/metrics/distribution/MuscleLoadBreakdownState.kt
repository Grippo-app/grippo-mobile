package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class MuscleLoadBreakdownState(
    val entries: ImmutableList<MuscleLoadEntryState>,
)
