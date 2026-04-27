package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable

@Immutable
public data class ExerciseDistributionEntryState<T>(
    val key: T,
    val value: Float,
)
