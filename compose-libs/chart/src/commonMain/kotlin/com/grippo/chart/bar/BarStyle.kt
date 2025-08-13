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
    val layout: Layout = Layout(),
    val grid: Grid = Grid(),
    val yAxis: YAxis = YAxis(),
    val xAxis: XAxis = XAxis(),
    val bars: Bars = Bars(),
    val values: Values = Values(),
    val target: Target? = null,
) {
    @Immutable
    public data class Layout(
        val padding: Dp = 12.dp,
        val labelPadding: Dp = 6.dp,
    )

    @Immutable
    public data class Grid(
        val show: Boolean = true,
        val color: Color = Color(0x22FFFFFF),
        val strokeWidth: Dp = 1.dp,
    )

    @Immutable
    public data class YAxis(
        val show: Boolean = true,
        val ticks: Int = 4,
        val textStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
        val showLine: Boolean = false,
        val axisLineColor: Color = Color(0x33FFFFFF),
        val axisLineWidth: Dp = 1.dp,
        val formatter: (Float, BarData) -> String = { v, d ->
            val unit = d.yUnit?.let { " ${it}" } ?: ""
            v.roundToInt().toString() + unit
        },
    )

    @Immutable
    public data class XAxis(
        val show: Boolean = true,
        val textStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
        val showBaseline: Boolean = false,
        val baselineColor: Color = Color(0x33FFFFFF),
        val baselineWidth: Dp = 1.dp,
    )

    @Immutable
    public data class Bars(
        val width: Dp = 16.dp,
        val spacing: Dp = 8.dp,
        val corner: Dp = 8.dp,
        val brushProvider: ((BarEntry, Size, Rect) -> Brush)? = null, // overrides entry.color
        val strokeWidth: Dp = 0.dp,
        val strokeColor: Color = Color.Transparent,
    )

    public enum class ValuePlacement { Inside, Outside, Above }

    @Immutable
    public data class Values(
        val show: Boolean = true,
        val textStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
        val formatter: (Float, BarData) -> String = { v, _ -> v.roundToInt().toString() },
        val placement: ValuePlacement = ValuePlacement.Above,
        val minInnerPadding: Dp = 6.dp,
        val insideColor: Color? = null, // null -> auto contrast
    )

    @Immutable
    public data class Target(
        val value: Float,
        val color: Color = Color(0x44FFFFFF),
        val width: Dp = 1.dp,
    )
}