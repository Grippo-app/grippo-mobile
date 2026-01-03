package com.grippo.data.features.api.metrics.models

import kotlinx.datetime.LocalDateTime

public data class EstimatedOneRepMaxSeries(
    val entries: List<EstimatedOneRepMaxEntry>,
)

public data class EstimatedOneRepMaxEntry(
    val label: String,
    val value: Float,
    /**
     * 0..1 confidence of the estimate quality (data coverage + reps applicability).
     */
    val confidence: Float,
    /**
     * Bucket/session start timestamp (when available). Useful for smoothing and debugging.
     */
    val start: LocalDateTime,
    /**
     * How many training sessions contributed to this point.
     */
    val sampleCount: Int,
)
