package com.grippo.chart.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
public data class RadarStyle(
    // layout
    val padding: Dp = 12.dp,
    val labelPadding: Dp = 10.dp,

    // grid
    val levels: Int = 4,
    val gridAsPolygon: Boolean = true, // if false, draw concentric circles
    val gridColor: Color = Color(0x33FFFFFF),
    val gridStrokeWidth: Dp = 1.dp,

    // spokes (center → vertices)
    val showSpokes: Boolean = true,
    val spokeColor: Color = Color(0x33FFFFFF),
    val spokeStrokeWidth: Dp = 1.dp,

    // data polygon
    val strokeWidth: Dp = 2.dp,
    val strokeColor: Color = Color(0xFFB049F8),
    val fillBrush: Brush? = Brush.radialGradient(
        listOf(Color(0xFFB049F8).copy(alpha = 0.35f), Color.Transparent)
    ),
    val showVertexDots: Boolean = true,
    val vertexDotRadius: Dp = 3.dp,
    val vertexDotColor: Color? = null, // null → use strokeColor

    // labels
    val showAxisLabels: Boolean = true,
    val labelStyle: TextStyle = TextStyle(color = Color(0x77FFFFFF)),

    // numeric rings labels
    val showLevelLabels: Boolean = false,
    val levelLabelStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
    val levelFormatter: (Float) -> String = { v -> "${(v * 100f).roundToInt()}%" },

    // values near vertices
    val showVertexValues: Boolean = false,
    val vertexValueStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
    val vertexValueFormatter: (Float) -> String = { v -> "${(v * 100f).roundToInt()}%" },

    // orientation
    val startAngleDeg: Float = -90f, // start at top
    val clockwise: Boolean = true,
)