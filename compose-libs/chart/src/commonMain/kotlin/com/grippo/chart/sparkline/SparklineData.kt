package com.grippo.chart.sparkline

import androidx.compose.runtime.Immutable

@Immutable
public data class SparklinePoint(
    val x: Float,
    val y: Float,
    val label: String? = null,
)

public data class SparklineData(
    val points: List<SparklinePoint>,
)