package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class VolumeSeriesState(
    val entries: ImmutableList<VolumeSeriesEntryState>,
)

@Immutable
public data class VolumeSeriesEntryState(
    val label: String,
    val value: Float,
)

public fun stubVolumeSeries(): VolumeSeriesState {
    val entries = listOf(
        VolumeSeriesEntryState(label = "Ex1", value = 120f),
        VolumeSeriesEntryState(label = "Ex2", value = 95f),
        VolumeSeriesEntryState(label = "Ex3", value = 150f),
        VolumeSeriesEntryState(label = "Ex4", value = 80f),
        VolumeSeriesEntryState(label = "Ex5", value = 160f),
    ).toPersistentList()

    return VolumeSeriesState(entries = entries)
}
