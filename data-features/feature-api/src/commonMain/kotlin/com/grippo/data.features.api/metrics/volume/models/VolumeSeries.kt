package com.grippo.data.features.api.metrics.volume.models

public data class VolumeSeries(
    val entries: List<VolumeSeriesEntry>,
)

public data class VolumeSeriesEntry(
    val label: String,
    val value: Float,
)
