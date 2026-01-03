package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class MuscleLoadTimelineState(
    val rows: Int,
    val cols: Int,
    val values01: List<Float>,
    val rowLabels: ImmutableList<String>,
    val colLabels: ImmutableList<String>,
)

public fun stubTemporalHeatmapState(): MuscleLoadTimelineState {
    val rows = 3
    val cols = 4
    val values = listOf(
        0.15f, 0.65f, 0.35f, 0.80f,
        0.30f, 0.45f, 0.55f, 0.25f,
        0.60f, 0.20f, 0.70f, 0.10f,
    )

    return MuscleLoadTimelineState(
        rows = rows,
        cols = cols,
        values01 = values,
        rowLabels = listOf("Chest", "Back", "Legs").toPersistentList(),
        colLabels = listOf("W1", "W2", "W3", "W4").toPersistentList(),
    )
}
