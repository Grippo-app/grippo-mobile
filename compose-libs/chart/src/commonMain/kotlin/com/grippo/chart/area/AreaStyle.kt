package com.grippo.chart.area

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
public data class AreaStyle(
    val layout: Layout = Layout(),
    val grid: Grid = Grid(),
    val yAxis: YAxis = YAxis(),
    val xAxis: XAxis = XAxis(),
    val line: Line = Line(),
    val glow: Glow = Glow(),
    val fill: Fill? = Fill(),            // null to disable fill
    val dots: Dots = Dots(),
    val extrema: Extrema = Extrema(),
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
        val formatter: (Float, AreaData) -> String = { v, d ->
            val unit = d.yUnit?.let { " ${it}" } ?: ""
            v.roundToInt().toString() + unit
        },
    )

    @Immutable
    public data class XAxis(
        val show: Boolean = false,
        val textStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
    )

    @Immutable
    public data class Line(
        val strokeWidth: Dp = 2.dp,
        val color: Color = Color(0xFF00E6A7),
        val brushProvider: ((Size) -> Brush)? = null, // overrides color
        val curved: Boolean = true,
        val curveSmoothness: Float = 0.20f, // 0..0.5
        val clampOvershoot: Boolean = true,
    )

    @Immutable
    public data class Glow(
        val width: Dp = 8.dp,                 // 0.dp to disable
        val color: Color? = null,             // null -> use line.color with low alpha
    )

    @Immutable
    public data class Fill(
        val brushProvider: (Size) -> Brush = { sz ->
            Brush.verticalGradient(
                0f to Color(0xFF00E6A7).copy(alpha = 0.22f),
                1f to Color(0xFF00E6A7).copy(alpha = 0.00f),
                startY = 0f,
                endY = sz.height
            )
        },
    )

    @Immutable
    public data class Dots(
        val show: Boolean = false,
        val radius: Dp = 2.dp,
        val color: Color? = null,
    )

    @Immutable
    public data class Extrema(
        val show: Boolean = true,
        val textStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
        val markerRadius: Dp = 3.dp,
        val markerColor: Color? = null,
    )
}