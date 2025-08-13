package com.grippo.chart.bar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class BarEntry(
    val label: String,
    val value: Float,
    val color: Color,
)

@Immutable
public data class BarData(
    val items: List<BarEntry>,
    val xName: String? = null,
    val yName: String? = null,
    val yUnit: String? = null, // e.g., "kg", "reps"
)