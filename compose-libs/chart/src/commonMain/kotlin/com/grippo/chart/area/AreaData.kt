package com.grippo.chart.area

import androidx.compose.runtime.Immutable

@Immutable
public data class AreaPoint(val x: Float, val y: Float)

@Immutable
public data class AreaData(
    val points: List<AreaPoint>,
    val xLabels: List<Pair<Float, String>> = emptyList(), // axis labels anchored at x-values
    val xName: String? = null,                            // optional axis name
    val yName: String? = null,                            // optional axis name
    val yUnit: String? = null,                            // unit suffix like "kg", "min"
)