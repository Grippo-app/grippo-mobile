package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.EstimatedOneRepMaxEntryState
import com.grippo.core.state.metrics.EstimatedOneRepMaxState
import com.grippo.data.features.api.metrics.models.EstimatedOneRepMaxSeries
import kotlinx.collections.immutable.toPersistentList

public fun EstimatedOneRepMaxSeries?.toState(): EstimatedOneRepMaxState? {
    val entries = this?.entries
        ?.map { entry ->
            EstimatedOneRepMaxEntryState(
                label = entry.label,
                value = entry.value,
            )
        }
        ?.toPersistentList()
        ?: return null

    if (entries.isEmpty()) return null

    return EstimatedOneRepMaxState(entries = entries)
}
