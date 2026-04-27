package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable

@Immutable
public data class MuscleLoadBreakdownState(
    val entries: List<MuscleLoadEntryState>,
)
