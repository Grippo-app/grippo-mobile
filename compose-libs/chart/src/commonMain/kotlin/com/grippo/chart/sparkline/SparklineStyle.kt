package com.grippo.chart.sparkline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class SparklineStyle(
    // line
    val stroke: Dp = 2.dp,
    val color: Color = Color(0xFF6AA9FF),
    val lineBrush: ((Rect) -> Brush)? = null, // overrides color
    val curved: Boolean = true,
    val curveSmoothness: Float = 0.25f, // Catmull–Rom → Bezier factor (0..0.5)
    val clampOvershoot: Boolean = true,

    // fill under line; set to null to disable
    val fill: ((Rect) -> Brush)? = { rect ->
        Brush.verticalGradient(
            0f to Color(0xFF6AA9FF).copy(alpha = 0.18f),
            1f to Color(0xFF6AA9FF).copy(alpha = 0.00f),
            startY = rect.top,
            endY = rect.bottom
        )
    },

    // baseline (subtle reference line)
    val showBaseline: Boolean = false,
    val baselineValue: Float? = null, // null → min value
    val baselineColor: Color = Color(0x33FFFFFF),
    val baselineWidth: Dp = 1.dp,

    // dots
    val showDots: Boolean = false,
    val dotRadius: Dp = 2.dp,
    val dotColor: Color? = null,

    // min/max markers
    val showMinMax: Boolean = true,
    val minColor: Color = Color(0xFFFF7A33),
    val maxColor: Color = Color(0xFF00E6A7),
    val minMaxRadius: Dp = 2.5.dp,

    // layout
    val padding: Dp = 6.dp,
)
