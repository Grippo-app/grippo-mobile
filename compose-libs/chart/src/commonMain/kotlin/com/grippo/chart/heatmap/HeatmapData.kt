package com.grippo.chart.heatmap

import androidx.compose.runtime.Immutable

@Immutable
public data class HeatmapCell(
    val row: Int,
    val col: Int,
    val value01: Float
)

@Immutable
public data class HeatmapData(
    val rows: Int,
    val cols: Int,
    val cells: List<HeatmapCell>,
    val rowLabels: List<String> = emptyList(),
    val colLabels: List<String> = emptyList(),
    val rowDim: String? = null,
    val colDim: String? = null,
    val valueUnit: String? = null,
)