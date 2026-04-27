package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Immutable

@Immutable
public data class EstimatedOneRepMaxEntryState(
    val label: String,
    val value: Float,
    val confidence: Float,
)
