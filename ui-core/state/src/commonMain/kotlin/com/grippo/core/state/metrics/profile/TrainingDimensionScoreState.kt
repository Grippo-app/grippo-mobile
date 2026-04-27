package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable

@Immutable
public data class TrainingDimensionScoreState(
    val kind: TrainingDimensionKindState,
    val score: Int, // 0..100
)
