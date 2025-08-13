package com.grippo.chart.sparkline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class SparklineStyle(
    val layout: Layout = Layout(),
    val line: Line = Line(),
    val fill: Fill = Fill(),
    val baseline: Baseline = Baseline(),
    val dots: Dots = Dots(),
    val extremes: Extremes = Extremes(),
) {
    @Immutable
    public data class Layout(
        val padding: Dp = 6.dp,
    )

    @Immutable
    public data class Line(
        val stroke: Dp = 2.dp,
        val color: Color = Color(0xFF6AA9FF),
        val brush: ((Rect) -> Brush)? = null, // overrides color
        val curved: Boolean = true,
        val curveSmoothness: Float = 0.25f, // 0..0.5
        val clampOvershoot: Boolean = true,
    )

    @Immutable
    public data class Fill(
        val provider: ((Rect) -> Brush)? = { rect ->
            Brush.verticalGradient(
                0f to Color(0xFF6AA9FF).copy(alpha = 0.18f),
                1f to Color(0xFF6AA9FF).copy(alpha = 0.00f),
                startY = rect.top,
                endY = rect.bottom
            )
        }
    )

    @Immutable
    public data class Baseline(
        val show: Boolean = false,
        val value: Float? = null, // null -> min value
        val color: Color = Color(0x33FFFFFF),
        val width: Dp = 1.dp,
    )

    @Immutable
    public data class Dots(
        val show: Boolean = false,
        val radius: Dp = 2.dp,
        val color: Color? = null, // null -> line color
    )

    @Immutable
    public data class Extremes(
        val show: Boolean = true,
        val minColor: Color = Color(0xFFFF7A33),
        val maxColor: Color = Color(0xFF00E6A7),
        val radius: Dp = 2.5.dp,
    )
}