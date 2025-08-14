package com.grippo.chart.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@Immutable
public data class RadarStyle(
    val layout: Layout,
    val grid: Grid,
    val spokes: Spokes,
    val labels: Labels,
    val polygon: Polygon,
    val vertices: Vertices,
    val values: Values,
    val dataPolicy: DataPolicy,
) {
    @Immutable
    public data class Layout(
        val padding: Dp,
        val labelPadding: Dp,
        val startAngleDeg: Float,
        val clockwise: Boolean,
    )

    @Immutable
    public data class Grid(
        val levels: Int,
        val asPolygon: Boolean,
        val color: Color,
        val strokeWidth: Dp,
        val showLevelLabels: Boolean,
        val levelLabelStyle: TextStyle,
        val levelFormatter: (Float) -> String,
    )

    @Immutable
    public data class Spokes(
        val show: Boolean,
        val color: Color,
        val strokeWidth: Dp,
    )

    @Immutable
    public data class Labels(
        val show: Boolean,
        val textStyle: TextStyle,
    )

    @Immutable
    public data class Polygon(
        val strokeWidth: Dp,
        val strokeColorFallback: Color,
        val fillAlpha: Float,
    )

    @Immutable
    public data class Vertices(
        val show: Boolean,
        val radius: Dp,
        val colorOverride: Color?,
    )

    @Immutable
    public data class Values(
        val show: Boolean,
        val textStyle: TextStyle,
        val formatter: (Float, RadarData) -> String,
        val offset: Dp,
    )

    @Immutable
    public data class DataPolicy(
        val requireCompleteSeries: Boolean,
        val missingAsZero: Boolean,
    )
}