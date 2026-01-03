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
    val confidence: Float,
)

public fun stubEstimatedOneRepMax(): EstimatedOneRepMaxState {
    val entries = listOf(
        EstimatedOneRepMaxEntryState(label = "Ex1", value = 92f, confidence = 0.8f),
        EstimatedOneRepMaxEntryState(label = "Ex2", value = 95f, confidence = 0.8f),
        EstimatedOneRepMaxEntryState(label = "Ex3", value = 98f, confidence = 0.85f),
        EstimatedOneRepMaxEntryState(label = "Ex4", value = 101f, confidence = 0.9f),
        EstimatedOneRepMaxEntryState(label = "Ex5", value = 105f, confidence = 0.9f),
        EstimatedOneRepMaxEntryState(label = "Ex6", value = 110f, confidence = 0.95f),
        EstimatedOneRepMaxEntryState(label = "Ex7", value = 108f, confidence = 0.9f),
        EstimatedOneRepMaxEntryState(label = "Ex8", value = 112f, confidence = 0.95f),
    ).toPersistentList()

    return EstimatedOneRepMaxState(entries = entries)
}
