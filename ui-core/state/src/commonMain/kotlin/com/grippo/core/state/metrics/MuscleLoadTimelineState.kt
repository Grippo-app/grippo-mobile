package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Deprecated("")
@Immutable
public data class MuscleLoadTimelineState(
    val rows: Int,
    val cols: Int,
    val values: List<Float>,
    val values01: List<Float>,
    val normalization: MuscleLoadTimelineNormalizationState,
    val rowLabels: ImmutableList<String>,
    val colLabels: ImmutableList<String>,
    val colSessionsCount: ImmutableList<Int>,
)

@Immutable
public enum class MuscleLoadTimelineNormalizationState {
    Absolute,
    PerRow,
    PerColumn,
}

public fun stubMuscleLoadTimeline(): MuscleLoadTimelineState {
    val rows = 3
    val cols = 4
    val rawValues = listOf(
        120f, 240f, 180f, 300f,
        90f, 140f, 160f, 80f,
        200f, 60f, 220f, 40f,
    )
    val values = listOf(
        0.15f, 0.65f, 0.35f, 0.80f,
        0.30f, 0.45f, 0.55f, 0.25f,
        0.60f, 0.20f, 0.70f, 0.10f,
    )

    return MuscleLoadTimelineState(
        rows = rows,
        cols = cols,
        values = rawValues,
        values01 = values,
        normalization = MuscleLoadTimelineNormalizationState.PerColumn,
        rowLabels = listOf("Chest", "Back", "Legs").toPersistentList(),
        colLabels = listOf("W1", "W2", "W3", "W4").toPersistentList(),
        colSessionsCount = listOf(3, 2, 4, 1).toPersistentList(),
    )
}
