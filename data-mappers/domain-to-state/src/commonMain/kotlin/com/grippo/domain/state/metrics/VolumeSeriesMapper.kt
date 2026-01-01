package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.VolumeSeriesEntryState
import com.grippo.core.state.metrics.VolumeSeriesState
import com.grippo.data.features.api.metrics.models.VolumeSeries
import kotlinx.collections.immutable.toPersistentList

public fun VolumeSeries?.toState(): VolumeSeriesState? {
    val entries = this?.entries
        ?.map { entry -> VolumeSeriesEntryState(label = entry.label, value = entry.value) }
        ?.toPersistentList()
        ?: return null

    if (entries.isEmpty()) return null

    return VolumeSeriesState(entries = entries)
}
