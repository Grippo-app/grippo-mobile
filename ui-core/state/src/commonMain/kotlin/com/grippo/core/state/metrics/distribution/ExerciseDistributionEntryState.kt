package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable

@Immutable
public data class ExerciseDistributionEntryState<T>(
    val key: T,
    /** Raw total in the distribution's weighting unit, not a percentage. */
    val value: Double,
)
