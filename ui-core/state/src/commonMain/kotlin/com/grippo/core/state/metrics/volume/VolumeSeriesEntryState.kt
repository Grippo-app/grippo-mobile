package com.grippo.core.state.metrics.volume

import androidx.compose.runtime.Immutable

@Immutable
public data class VolumeSeriesEntryState(
    val label: String,
    val value: Float,
)
