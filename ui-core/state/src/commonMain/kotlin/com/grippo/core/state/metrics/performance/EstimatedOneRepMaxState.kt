package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Immutable
public data class EstimatedOneRepMaxState(
    val entries: ImmutableList<EstimatedOneRepMaxEntryState>,
)

public fun stubEstimatedOneRepMax(
    loadType: EstimatedOneRepMaxLoadTypeState = EstimatedOneRepMaxLoadTypeState.External,
): EstimatedOneRepMaxState {
    val dates = listOf(
        "2026-08-23T18:00", "2026-08-30T17:30", "2026-09-04T18:15", "2026-09-12T19:00",
        "2026-09-20T17:00", "2026-09-27T18:30", "2026-10-08T18:00", "2026-10-15T17:45",
    )
    val weights = when (loadType) {
        EstimatedOneRepMaxLoadTypeState.External -> listOf(80f, 82.5f, 85f, 87.5f, 90f, 95f, 92.5f, 97.5f)
        EstimatedOneRepMaxLoadTypeState.Effective -> listOf(55f, 57.5f, 60f, 62.5f, 65f, 70f, 67.5f, 72.5f)
    }
    val entries = weights.mapIndexed { index, weight ->
        EstimatedOneRepMaxEntryState(
            start = LocalDateTime.parse(dates[index]),
            value = weight * 1.15f,
            weight = weight,
            repetitions = 5,
            loadType = loadType,
        )
    }.toPersistentList()

    return EstimatedOneRepMaxState(entries = entries)
}
