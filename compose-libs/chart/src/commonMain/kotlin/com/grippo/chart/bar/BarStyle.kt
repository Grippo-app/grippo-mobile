package com.grippo.chart.bar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@Immutable
public data class BarStyle(
    val layout: Layout,
    val grid: Grid,
    val yAxis: YAxis,
    val xAxis: XAxis,
    val bars: Bars,
    val values: Values,
    val target: Target?,
) {
    @Immutable
    public data class Layout(
        val padding: Dp,
        val labelPadding: Dp,
    )

    @Immutable
    public data class Grid(
        val show: Boolean,
        val color: Color,
        val strokeWidth: Dp,
    )

    @Immutable
    public data class YAxis(
        val show: Boolean,
        val ticks: Int,
        val textStyle: TextStyle,
        val showLine: Boolean,
        val axisLineColor: Color,
        val axisLineWidth: Dp,
        val formatter: (Float, BarData) -> String,
    )

    @Immutable
    public data class XAxis(
        val show: Boolean,
        val textStyle: TextStyle,
        val showBaseline: Boolean,
        val baselineColor: Color,
        val baselineWidth: Dp,
    )

    @Immutable
    public data class Bars(
        val width: Dp,
        val spacing: Dp,
        val corner: Dp,
        val brushProvider: ((BarEntry, Size, Rect) -> Brush)?,
        val strokeWidth: Dp,
        val strokeColor: Color,
    )

    public enum class ValuePlacement { Inside, Outside, Above }

    @Immutable
    public data class Values(
        val show: Boolean,
        val textStyle: TextStyle,
        val formatter: (Float, BarData) -> String,
        val placement: ValuePlacement,
        val minInnerPadding: Dp,
        val insideColor: Color?,
    )

    @Immutable
    public data class Target(
        val value: Float,
        val color: Color,
        val width: Dp,
    )
}