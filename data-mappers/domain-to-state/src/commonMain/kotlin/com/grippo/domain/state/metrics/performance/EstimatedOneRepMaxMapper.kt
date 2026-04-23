package com.grippo.domain.state.metrics.performance

import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxEntryState
import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxState
import com.grippo.data.features.api.metrics.performance.EstimatedOneRepMaxSeries
import kotlinx.collections.immutable.toPersistentList

public fun EstimatedOneRepMaxSeries?.toState(): EstimatedOneRepMaxState? {
    val entries = this?.entries
        ?.map { entry ->
            EstimatedOneRepMaxEntryState(
                label = entry.label,
                value = entry.value,
                confidence = entry.confidence,
            )
        }
        ?.toPersistentList()
        ?: return null

    if (entries.isEmpty()) return null

    return EstimatedOneRepMaxState(entries = entries)
}
