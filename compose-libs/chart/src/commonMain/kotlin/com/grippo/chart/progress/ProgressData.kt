package com.grippo.chart.progress

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class ProgressChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Immutable
public data class ProgressData(
    val items: List<ProgressChartData>,
)
