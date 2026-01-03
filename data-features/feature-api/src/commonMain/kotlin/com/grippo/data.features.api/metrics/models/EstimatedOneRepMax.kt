package com.grippo.data.features.api.metrics.models

public data class EstimatedOneRepMaxSeries(
    val entries: List<EstimatedOneRepMaxEntry>,
)

public data class EstimatedOneRepMaxEntry(
    val label: String,
    val value: Float,
)
