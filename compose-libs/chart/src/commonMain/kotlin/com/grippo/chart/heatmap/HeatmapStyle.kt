package com.grippo.chart.heatmap

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
public data class HeatmapStyle(
    val rows: Int,
    val cols: Int,

    // layout
    val padding: Dp = 12.dp,
    val gap: Dp = 6.dp,
    val corner: Dp = 6.dp,
    val labelPadding: Dp = 6.dp,

    // labels
    val rowLabels: List<String> = emptyList(), // size == rows (optional)
    val colLabels: List<String> = emptyList(), // size == cols (optional)
    val labelStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),

    // legend
    val showLegend: Boolean = true,
    val legendHeight: Dp = 12.dp,
    val legendStops: List<Pair<Float, Color>>? = null, // normalized 0..1 stops; if null uses default cool-warm
    val legendLabelStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
    val legendMinText: ((Float) -> String)? = null, // if null, shows 0%
    val legendMaxText: ((Float) -> String)? = null, // if null, shows 100%

    // color scale
    val colorScale: (Float) -> Color = { v -> defaultCoolWarm(v) },
    val autoNormalize: Boolean = false, // if true, normalize values by detected min/max
    val missingCellColor: Color? = Color(0x11111111), // background for missing cells; null disables

    // cells
    val showCellBorders: Boolean = false,
    val borderColor: Color = Color(0x22FFFFFF),
    val borderWidth: Dp = 1.dp,

    // values in cells
    val showValues: Boolean = false,
    val valueTextStyle: TextStyle = TextStyle(color = Color.White),
    val valueFormatter: (Float) -> String = { p -> "${(p * 100f).roundToInt()}%" },
)

// Default cool→magenta→warm palette used only by this chart
public fun defaultCoolWarm(vIn: Float): Color {
    val v = vIn.coerceIn(0f, 1f)
    val c1 = Color(0xFF3A86FF) // blue
    val c2 = Color(0xFFB049F8) // magenta
    val c3 = Color(0xFFFF7A33) // orange
    return if (v < 0.5f) lerpColor(c1, c2, v * 2f) else lerpColor(c2, c3, (v - 0.5f) * 2f)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = (a.red + (b.red - a.red) * k),
        green = (a.green + (b.green - a.green) * k),
        blue = (a.blue + (b.blue - a.blue) * k),
        alpha = (a.alpha + (b.alpha - a.alpha) * k)
    )
}
