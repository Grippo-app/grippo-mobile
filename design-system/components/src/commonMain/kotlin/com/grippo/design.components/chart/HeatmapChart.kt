package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.heatmap.HeatmapCell
import com.grippo.chart.heatmap.HeatmapChart
import com.grippo.chart.heatmap.HeatmapData
import com.grippo.chart.heatmap.HeatmapStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun HeatmapChart(
    modifier: Modifier = Modifier
) {
    val charts = AppTokens.colors.charts

    val rows = 6
    val cols = 7
    val labelsRow = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
    val labelsCol = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val cells = buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val base = (r + 1) * (c + 1)
                val v = ((base % 10) / 10f).coerceIn(0f, 1f)
                add(HeatmapCell(r, c, v))
            }
        }
    }

    val data = HeatmapData(
        rows = rows,
        cols = cols,
        cells = cells,
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
            padding = 12.dp,
            gap = 6.dp,
            corner = 6.dp,
            labelPadding = 6.dp
        ),
        labels = HeatmapStyle.Labels(
            showRowLabels = true,
            showColLabels = true,
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary)
        ),
        legend = HeatmapStyle.Legend(
            show = true,
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
        cells = HeatmapStyle.Cells(
            showBorders = false,
            borderColor = AppTokens.colors.divider.default,
            borderWidth = 1.dp
        ),
        values = HeatmapStyle.Values(
            show = false,
            textStyle = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.primary),
            formatter = { p, _ -> "${(p * 100f).roundToInt()}%" }
        )
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

        val cells = buildList {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val base = (r + 1) * (c + 1)
                    val v = ((base % 10) / 10f).coerceIn(0f, 1f)
                    add(HeatmapCell(r, c, v))
                }
            }
        }

        HeatmapData(
            rows = rows,
            cols = cols,
            cells = cells,
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