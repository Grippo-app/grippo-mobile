package com.grippo.chart.heatmap

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

private fun scaleStops(): List<Pair<Float, Color>> = listOf(
    0f to Color(0xFF3A86FF), 0.5f to Color(0xFFB049F8), 1f to Color(0xFFFF7A33)
)

private fun colorScaleOf(stops: List<Pair<Float, Color>>): (Float) -> Color = { tIn ->
    val t = tIn.coerceIn(0f, 1f)
    val sorted = stops.sortedBy { it.first }
    val idx = sorted.indexOfLast { it.first <= t }.coerceAtLeast(0)
    val a = sorted[idx]
    val b = sorted.getOrNull(idx + 1) ?: a
    if (a.first == b.first) a.second else {
        val k = (t - a.first) / (b.first - a.first)
        Color(
            red = a.second.red + (b.second.red - a.second.red) * k,
            green = a.second.green + (b.second.green - a.second.green) * k,
            blue = a.second.blue + (b.second.blue - a.second.blue) * k,
            alpha = a.second.alpha + (b.second.alpha - a.second.alpha) * k,
        )
    }
}

private fun hmData(rows: Int = 6, cols: Int = 7): HeatmapData = HeatmapData.fromRows(
    values01 = List(rows) { r -> List(cols) { c -> (((r + 1) * (c + 2)) % 10) / 10f } },
    rowLabels = List(rows) { listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")[it] },
    colLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
)

private fun hmFew(): HeatmapData = HeatmapData.fromRows(
    values01 = listOf(
        listOf(0.1f, 0.6f, 0.2f),
        listOf(0.3f, 0.9f, 0.4f),
    ),
    rowLabels = listOf("Upper", "Lower"),
    colLabels = listOf("Mon", "Wed", "Fri")
)

private fun hmStyle(adaptiveCols: Boolean): HeatmapStyle = HeatmapStyle(
    layout = HeatmapStyle.Layout(gap = 1.dp, corner = 3.dp, labelPadding = 4.dp),
    rowLabels = HeatmapStyle.AxisLabels.ShowAll(
        textStyle = TextStyle(color = Color(0xFF333333))
    ),
    colLabels = if (adaptiveCols) HeatmapStyle.AxisLabels.Adaptive(
        textStyle = TextStyle(color = Color(0xFF333333)), minGapDp = 1.dp
    ) else HeatmapStyle.AxisLabels.ShowAll(
        textStyle = TextStyle(color = Color(0xFF333333))
    ),
    legend = HeatmapStyle.Legend.Visible(
        height = 8.dp,
        stops = scaleStops(),
        labelStyle = TextStyle(color = Color(0xFF808080)),
        minText = { "0%" }, maxText = { "100%" }
    ),
    palette = HeatmapStyle.Palette(
        colorScale = colorScaleOf(scaleStops()),
        autoNormalize = false,
        missingCellColor = Color(0x11000000)
    ),
    borders = HeatmapStyle.Borders.None,
    values = HeatmapStyle.Values.None
)

@Preview
@Composable
private fun PreviewHeatmapShowAll() {
    HeatmapChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = hmData(),
        style = hmStyle(adaptiveCols = false)
    )
}

@Preview
@Composable
private fun PreviewHeatmapAdaptive() {
    HeatmapChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = hmData(),
        style = hmStyle(adaptiveCols = true)
    )
}

@Preview
@Composable
private fun PreviewHeatmapFewShowAll() {
    HeatmapChart(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        data = hmFew(),
        style = hmStyle(adaptiveCols = false)
    )
}



