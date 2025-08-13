package com.grippo.chart.heatmap

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
public data class HeatmapStyle(
    val layout: Layout = Layout(),
    val labels: Labels = Labels(),
    val legend: Legend = Legend(),
    val palette: Palette = Palette(),
    val cells: Cells = Cells(),
    val values: Values = Values(),
) {
    @Immutable
    public data class Layout(
        val padding: Dp = 12.dp,
        val gap: Dp = 6.dp,
        val corner: Dp = 6.dp,
        val labelPadding: Dp = 6.dp,
    )

    @Immutable
    public data class Labels(
        val showRowLabels: Boolean = true,
        val showColLabels: Boolean = true,
        val textStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
    )

    @Immutable
    public data class Legend(
        val show: Boolean = true,
        val height: Dp = 12.dp,
        val stops: List<Pair<Float, Color>>? = null, // 0..1
        val labelStyle: TextStyle = TextStyle(color = Color(0x66FFFFFF)),
        val minText: ((Float) -> String)? = null,
        val maxText: ((Float) -> String)? = null,
    )

    @Immutable
    public data class Palette(
        val colorScale: (Float) -> Color = { v -> defaultCoolWarm(v) },
        val autoNormalize: Boolean = false,
        val missingCellColor: Color? = Color(0x11FFFFFF),
    )

    @Immutable
    public data class Cells(
        val showBorders: Boolean = false,
        val borderColor: Color = Color(0x22FFFFFF),
        val borderWidth: Dp = 1.dp,
    )

    @Immutable
    public data class Values(
        val show: Boolean = false,
        val textStyle: TextStyle = TextStyle(color = Color.White),
        val formatter: (Float, HeatmapData) -> String = { p, d ->
            val pct = (p * 100f).roundToInt().toString() + "%"
            d.valueUnit?.let { "$pct $it" } ?: pct
        },
    )
}