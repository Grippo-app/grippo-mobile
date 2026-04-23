package com.grippo.data.features.api.metrics.volume

public data class VolumeSeries(
    val entries: List<VolumeSeriesEntry>,
)

public data class VolumeSeriesEntry(
    val label: String,
    val value: Float,
)
