package com.grippo.domain.state.metrics.performance

import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxEntryState
import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxLoadTypeState
import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxState
import com.grippo.data.features.api.metrics.performance.models.EstimatedOneRepMaxLoadType
import com.grippo.data.features.api.metrics.performance.models.EstimatedOneRepMaxSeries
import kotlinx.collections.immutable.toPersistentList

public fun EstimatedOneRepMaxSeries?.toState(): EstimatedOneRepMaxState? {
    val entries = this?.entries
        ?.map { entry ->
            EstimatedOneRepMaxEntryState(
                start = entry.start,
                value = entry.value,
                weight = entry.weight,
                repetitions = entry.repetitions,
                loadType = when (entry.loadType) {
                    EstimatedOneRepMaxLoadType.External -> EstimatedOneRepMaxLoadTypeState.External
                    EstimatedOneRepMaxLoadType.Effective -> EstimatedOneRepMaxLoadTypeState.Effective
                },
            )
        }
        ?.toPersistentList()
        ?: return null

    if (entries.isEmpty()) return null

    return EstimatedOneRepMaxState(entries = entries)
}
