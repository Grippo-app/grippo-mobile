package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable

@Immutable
public data class MuscleLoadDominanceState(
    val top1SharePercent: Float,
    val top2SharePercent: Float,
)
