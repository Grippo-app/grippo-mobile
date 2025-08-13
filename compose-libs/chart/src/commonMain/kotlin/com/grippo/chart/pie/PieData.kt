package com.grippo.chart.pie

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class PieSlice(
    val id: String,
    val label: String,
    val value: Float,
    val color: Color,
)

@Immutable
public data class PieData(
    val slices: List<PieSlice>,
    val valueUnit: String? = null,
)