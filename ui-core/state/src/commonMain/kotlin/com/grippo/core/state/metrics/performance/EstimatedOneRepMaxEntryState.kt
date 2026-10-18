package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
public data class EstimatedOneRepMaxEntryState(
    val start: LocalDateTime,
    val value: Float,
    val weight: Float,
    val repetitions: Int,
    val loadType: EstimatedOneRepMaxLoadTypeState,
)
