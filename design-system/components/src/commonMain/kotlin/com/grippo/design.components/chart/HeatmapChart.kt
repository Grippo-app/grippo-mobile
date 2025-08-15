package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.heatmap.HeatmapChart
import com.grippo.chart.heatmap.HeatmapData
import com.grippo.chart.heatmap.HeatmapStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun HeatmapChart(
    modifier: Modifier = Modifier
) {
    val charts = AppTokens.colors.charts

    val rows = 6
    val cols = 7
    val labelsRow = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
    val labelsCol = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val data = HeatmapData.fromRows(
        values01 = List(rows) { r ->
            List(cols) { c ->
                val base = (r + 1) * (c + 1)
                ((base % 10) / 10f)
            }
        },
        rowLabels = labelsRow,
        colLabels = labelsCol,
        rowDim = "Muscle Group",
        colDim = "Day",
        valueUnit = null,
    )

    fun scaleColorOf(stops: List<Pair<Float, Color>>): (Float) -> Color = { tIn ->
        val t = tIn.coerceIn(0f, 1f)
        if (stops.isEmpty()) Color.Unspecified
        else {
            val sorted = stops.sortedBy { it.first }
            val idx = sorted.indexOfLast { it.first <= t }.coerceAtLeast(0)
            val a = sorted[idx]
            val b = sorted.getOrNull(idx + 1) ?: a
            if (a.first == b.first) a.second
            else {
                val k = (t - a.first) / (b.first - a.first)
                Color(
                    red = a.second.red + (b.second.red - a.second.red) * k,
                    green = a.second.green + (b.second.green - a.second.green) * k,
                    blue = a.second.blue + (b.second.blue - a.second.blue) * k,
                    alpha = a.second.alpha + (b.second.alpha - a.second.alpha) * k,
                )
            }
        }
    }

    val style = HeatmapStyle(
        layout = HeatmapStyle.Layout(
            gap = 6.dp,
            corner = 6.dp,
            labelPadding = 6.dp
        ),
        rowLabels = HeatmapStyle.AxisLabels.ShowAll(
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary)
        ),
        colLabels = HeatmapStyle.AxisLabels.Adaptive(
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary),
            minGapDp = 1.dp
        ),
        legend = HeatmapStyle.Legend.Visible(
            height = 12.dp,
            stops = charts.heatmap.scaleStops,
            labelStyle = AppTokens.typography.b11Reg()
                .copy(color = AppTokens.colors.text.secondary),
            minText = { "0%" },
            maxText = { "100%" }
        ),
        palette = HeatmapStyle.Palette(
            colorScale = scaleColorOf(charts.heatmap.scaleStops),
            autoNormalize = false,
            missingCellColor = charts.heatmap.missingCell
        ),
        borders = HeatmapStyle.Borders.None,
        values = HeatmapStyle.Values.None
    )

    HeatmapChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun HeatmapChartPreview() {
    PreviewContainer {
        val rows = 6
        val cols = 7
        val labelsRow = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
        val labelsCol = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")



        HeatmapData.fromRows(
            values01 = List(rows) { r ->
                List(cols) { c ->
                    val base = (r + 1) * (c + 1)
                    ((base % 10) / 10f)
                }
            },
            rowLabels = labelsRow,
            colLabels = labelsCol,
            rowDim = "Muscle Group",
            colDim = "Day",
            valueUnit = null,
        )

        HeatmapChart(
            modifier = Modifier.size(300.dp)
        )
    }
}