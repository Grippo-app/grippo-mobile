package com.grippo.chart.bar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
public data class BarStyle(
    // layout
    val padding: Dp = 12.dp,
    val barWidth: Dp = 16.dp,
    val spacing: Dp = 8.dp,
    val corner: Dp = 8.dp,
    val labelPadding: Dp = 6.dp,

    // grid
    val showGrid: Boolean = true,
    val gridColor: Color = Color(0x22FFFFFF),
    val gridStrokeWidth: Dp = 1.dp,

    // axes
    val showYAxis: Boolean = true,
    val yAxisTicks: Int = 4,
    val yAxisTextStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
    val yValueFormatter: (Float) -> String = { v -> v.roundToInt().toString() },
    val showYAxisLine: Boolean = false,
    val axisLineColor: Color = Color(0x33FFFFFF),
    val axisLineWidth: Dp = 1.dp,

    val showXLabels: Boolean = true,
    val xAxisTextStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),

    // bars
    val barBrush: ((BarEntry, Size, Rect) -> Brush)? = null, // if provided, overrides entry.color
    val barStrokeWidth: Dp = 0.dp,
    val barStrokeColor: Color = Color.Transparent,

    // values on top of bars
    val showValueLabels: Boolean = true,
    val valueLabelTextStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
    val valueFormatter: (Float) -> String = { v -> v.roundToInt().toString() },
)