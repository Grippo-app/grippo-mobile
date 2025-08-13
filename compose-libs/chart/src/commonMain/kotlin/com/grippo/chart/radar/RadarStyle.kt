package com.grippo.chart.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
public data class RadarStyle(
    val layout: Layout = Layout(),
    val grid: Grid = Grid(),
    val spokes: Spokes = Spokes(),
    val labels: Labels = Labels(),
    val polygon: Polygon = Polygon(),
    val vertices: Vertices = Vertices(),
    val values: Values = Values(),
    val dataPolicy: DataPolicy = DataPolicy(),
) {
    @Immutable
    public data class Layout(
        val padding: Dp = 12.dp,
        val labelPadding: Dp = 12.dp,
        val startAngleDeg: Float = -90f,
        val clockwise: Boolean = true,
    )

    @Immutable
    public data class Grid(
        val levels: Int = 5,
        val asPolygon: Boolean = true, // false -> concentric circles
        val color: Color = Color(0x33FFFFFF),
        val strokeWidth: Dp = 1.dp,
        val showLevelLabels: Boolean = false,
        val levelLabelStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
        val levelFormatter: (Float) -> String = { v -> "${(v * 100f).roundToInt()}%" },
    )

    @Immutable
    public data class Spokes(
        val show: Boolean = true,
        val color: Color = Color(0x22FFFFFF),
        val strokeWidth: Dp = 1.dp,
    )

    @Immutable
    public data class Labels(
        val show: Boolean = true,
        val textStyle: TextStyle = TextStyle(color = Color(0x77FFFFFF)),
    )

    @Immutable
    public data class Polygon(
        val strokeWidth: Dp = 2.dp,
        val strokeColorFallback: Color = Color(0xFFB049F8),
        val fillAlpha: Float = 0.35f, // fill uses series.color with this alpha
    )

    @Immutable
    public data class Vertices(
        val show: Boolean = true,
        val radius: Dp = 3.dp,
        val colorOverride: Color? = null, // null -> use series.color
    )

    @Immutable
    public data class Values(
        val show: Boolean = false,
        val textStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
        val formatter: (Float, RadarData) -> String = { v, d ->
            val pct = (v.coerceIn(0f, 1f) * 100f).roundToInt().toString() + "%"
            d.valueUnit?.let { "$pct ${it}" } ?: pct
        },
        val offset: Dp = 8.dp, // distance from vertex
    )

    @Immutable
    public data class DataPolicy(
        val requireCompleteSeries: Boolean = false, // if true, skip drawing series with any missing axis
        val missingAsZero: Boolean = true,          // if false, missing axis is skipped in polygon
    )
}