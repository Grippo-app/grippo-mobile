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
