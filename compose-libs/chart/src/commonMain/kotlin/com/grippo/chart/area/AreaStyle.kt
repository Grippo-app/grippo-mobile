package com.grippo.chart.area

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class AreaStyle(
    val grid: Grid,
    val yAxis: YAxis,
    val xAxis: XAxis,
    val line: Line,
    val glow: Glow,
    val fill: Fill?,
    val dots: Dots,
    val extrema: Extrema,
    val labelPadding: Dp = 6.dp,
) {
    @Immutable
    public data class Grid(
        val show: Boolean,
        val color: Color,
        val strokeWidth: Dp
    )

    @Immutable
    public data class YAxis(
        val show: Boolean,
        val targetTicks: Int,
        val textStyle: TextStyle,
        val showLine: Boolean,
        val axisLineColor: Color,
        val axisLineWidth: Dp,
        val formatter: (Float, AreaData) -> String
    )

    @Immutable
    public data class XAxis(
        val show: Boolean,
        val textStyle: TextStyle,
        val minGapDp: Dp,
        val showAll: Boolean
    )

    @Immutable
    public data class Line(
        val strokeWidth: Dp,
        val color: Color,
        val brushProvider: ((Size) -> Brush)?,
        val curved: Boolean,
        val curveSmoothness: Float,
        val clampOvershoot: Boolean
    )

    @Immutable
    public data class Glow(
        val width: Dp,
        val color: Color?
    )

    @Immutable
    public data class Fill(
        val brushProvider: (Size) -> Brush
    )

    @Immutable
    public data class Dots(
        val show: Boolean,
        val radius: Dp,
        val color: Color?
    )

    @Immutable
    public data class Extrema(
        val show: Boolean,
        val textStyle: TextStyle,
        val markerRadius: Dp,
        val markerColor: Color?
    )
}