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
    // line
    val strokeWidth: Dp = 2.dp,
    val lineColor: Color = Color(0xFF00E6A7),
    val lineBrush: ((Size) -> Brush)? = null, // if provided, overrides lineColor
    val curved: Boolean = true,
    val curveSmoothness: Float = 0.20f, // Catmull–Rom → Bezier factor (0..0.5)
    val clampOvershoot: Boolean = true, // try to keep curves monotone-ish

    // line glow (soft halo for dark themes). Set width to 0.dp to disable
    val lineGlowWidth: Dp = 8.dp,
    val lineGlowColor: Color? = null, // if null, uses lineColor with low alpha

    // fill under the line; set to null to disable
    val fill: ((Size) -> Brush)? = { sz ->
        Brush.verticalGradient(
            0f to Color(0xFF00E6A7).copy(alpha = 0.22f),
            1f to Color(0xFF00E6A7).copy(alpha = 0.00f),
            startY = 0f,
            endY = sz.height
        )
    },

    // grid
    val showGrid: Boolean = true,
    val gridColor: Color = Color(0x22FFFFFF),
    val gridStrokeWidth: Dp = 1.dp,

    // axes (no Material deps)
    val showYAxis: Boolean = true,
    val yAxisTicks: Int = 4,
    val yAxisTextStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
    val yValueFormatter: (Float) -> String = { v -> v.roundToInt().toString() },
    val showYAxisLine: Boolean = false,
    val axisLineColor: Color = Color(0x33FFFFFF),
    val axisLineWidth: Dp = 1.dp,

    val showXAxis: Boolean = false,
    val xAxisTextStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
    val xLabels: List<Pair<Float, String>> = emptyList(), // pairs of x-value to label text

    // layout
    val padding: Dp = 12.dp,
    val labelPadding: Dp = 6.dp,

    // points
    val showDots: Boolean = false,
    val dotRadius: Dp = 2.dp,
    val dotColor: Color? = null,

    // extrema markers
    val showExtrema: Boolean = true,
    val extremaTextStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
)