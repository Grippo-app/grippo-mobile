package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class EstimatedOneRepMaxState(
    val entries: ImmutableList<EstimatedOneRepMaxEntryState>,
)

@Immutable
public data class EstimatedOneRepMaxEntryState(
    val label: String,
    val value: Float,
)

public fun stubEstimatedOneRepMax(): EstimatedOneRepMaxState {
    val entries = listOf(
        EstimatedOneRepMaxEntryState(label = "Ex1", value = 92f),
        EstimatedOneRepMaxEntryState(label = "Ex2", value = 95f),
        EstimatedOneRepMaxEntryState(label = "Ex3", value = 98f),
        EstimatedOneRepMaxEntryState(label = "Ex4", value = 101f),
        EstimatedOneRepMaxEntryState(label = "Ex5", value = 105f),
        EstimatedOneRepMaxEntryState(label = "Ex6", value = 110f),
        EstimatedOneRepMaxEntryState(label = "Ex7", value = 108f),
        EstimatedOneRepMaxEntryState(label = "Ex8", value = 112f),
    ).toPersistentList()

    return EstimatedOneRepMaxState(entries = entries)
}
