package com.grippo.chart.area

import androidx.compose.runtime.Immutable

@Immutable
public data class AreaPoint(
    val x: Float,
    val y: Float,
    val xLabel: String? = null
)

@Immutable
public data class AreaData(
    val points: List<AreaPoint>
)