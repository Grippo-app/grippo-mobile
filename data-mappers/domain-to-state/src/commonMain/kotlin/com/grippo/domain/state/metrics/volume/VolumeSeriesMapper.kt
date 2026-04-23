package com.grippo.domain.state.metrics.volume

import com.grippo.core.state.metrics.volume.VolumeSeriesEntryState
import com.grippo.core.state.metrics.volume.VolumeSeriesState
import com.grippo.data.features.api.metrics.volume.VolumeSeries
import kotlinx.collections.immutable.toPersistentList

public fun VolumeSeries?.toState(): VolumeSeriesState? {
    val entries = this?.entries
        ?.map { entry -> VolumeSeriesEntryState(label = entry.label, value = entry.value) }
        ?.toPersistentList()
        ?: return null

    if (entries.isEmpty()) return null

    return VolumeSeriesState(entries = entries)
}
