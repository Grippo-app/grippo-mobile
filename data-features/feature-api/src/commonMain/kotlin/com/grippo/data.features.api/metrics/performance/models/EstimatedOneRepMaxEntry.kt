package com.grippo.data.features.api.metrics.performance.models

import kotlinx.datetime.LocalDateTime

public data class EstimatedOneRepMaxEntry(
    val start: LocalDateTime,
    val value: Float,
    /** Load used by the winning set, expressed on [loadType]'s basis. */
    val weight: Float,
    val repetitions: Int,
    val loadType: EstimatedOneRepMaxLoadType,
)
