package com.grippo.chart.sparkline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
public data class SparklineStyle(
    val layout: Layout,
    val line: Line,
    val fill: Fill?,
    val baseline: Baseline,
    val dots: Dots,
    val extremes: Extremes,
) {
    @Immutable
    public data class Layout(
        val padding: Dp,
    )

    @Immutable
    public data class Line(
        val stroke: Dp,
        val color: Color,
        val brush: ((Rect) -> Brush)?,
        val curved: Boolean,
        val curveSmoothness: Float,    // 0..0.5
        val clampOvershoot: Boolean,
    )

    @Immutable
    public data class Fill(
        val provider: ((Rect) -> Brush)?,
    )

    @Immutable
    public data class Baseline(
        val show: Boolean,
        val value: Float?,    // null → min value
        val color: Color,
        val width: Dp,
    )

    @Immutable
    public data class Dots(
        val show: Boolean,
        val radius: Dp,
        val color: Color?,
    )

    @Immutable
    public data class Extremes(
        val show: Boolean,
        val minColor: Color,
        val maxColor: Color,
        val radius: Dp,
    )
}