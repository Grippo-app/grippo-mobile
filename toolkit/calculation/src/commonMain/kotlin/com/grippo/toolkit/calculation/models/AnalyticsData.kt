package com.grippo.toolkit.calculation.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class MetricPoint(
    val label: String,
    val value: Float,
    val color: Color,
)

@Immutable
public data class MetricSeries(
    val points: List<MetricPoint>,
)

@Immutable
public data class MuscleLoadMatrix(
    val rows: Int,
    val cols: Int,
    val values01: List<Float>,
    val rowLabels: List<String> = emptyList(),
    val colLabels: List<String> = emptyList(),
)

public sealed interface Metric {
    public data object TONNAGE : Metric
    public data object REPS : Metric
}