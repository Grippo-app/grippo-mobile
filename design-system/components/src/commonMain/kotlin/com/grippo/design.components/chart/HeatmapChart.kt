package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.heatmap.HeatmapChart
import com.grippo.chart.heatmap.HeatmapData
import com.grippo.chart.heatmap.HeatmapStyle
import com.grippo.chart.heatmap.Matrix01
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public data class DSHeatmapData(
    val rows: Int,
    val cols: Int,
    val values01: List<Float>,
    val rowLabels: List<String> = emptyList(),
    val colLabels: List<String> = emptyList(),
    val rowDim: String? = null,
    val colDim: String? = null,
    val valueUnit: String? = null,
)

@Composable
public fun HeatmapChart(
    modifier: Modifier = Modifier,
    data: DSHeatmapData
) {
    val charts = AppTokens.colors.charts
    val palette = AppTokens.colors.palette

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
            gap = 1.dp,
            corner = 3.dp,
            labelPadding = 4.dp
        ),
        rowLabels = HeatmapStyle.AxisLabels.ShowAll(
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary)
        ),
        colLabels = HeatmapStyle.AxisLabels.Adaptive(
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary),
            minGapDp = 1.dp
        ),
        legend = HeatmapStyle.Legend.Visible(
            height = 8.dp,
            stops = palette.scaleStopsOrangeRed,
            labelStyle = AppTokens.typography.b11Reg()
                .copy(color = AppTokens.colors.text.secondary),
            minText = { "0%" },
            maxText = { "100%" }
        ),
        palette = HeatmapStyle.Palette(
            colorScale = scaleColorOf(palette.scaleStopsOrangeRed),
            autoNormalize = false,
            missingCellColor = charts.heatmap.missingCell
        ),
        borders = HeatmapStyle.Borders.None,
        values = HeatmapStyle.Values.None
    )

    HeatmapChart(
        modifier = modifier,
        data = remember(data) { data.toChart() },
        style = style
    )
}

private fun DSHeatmapData.toChart(): HeatmapData {
    val matrix = Matrix01.fromFlat(rows, cols, values01)
    return HeatmapData(
        matrix = matrix,
        rowLabels = rowLabels,
        colLabels = colLabels,
        rowDim = rowDim,
        colDim = colDim,
        valueUnit = valueUnit,
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

        val ds = DSHeatmapData(
            rows = rows,
            cols = cols,
            values01 = List(rows * cols) { idx ->
                val r = idx / cols
                val c = idx % cols
                val base = (r + 1) * (c + 1)
                ((base % 10) / 10f)
            },
            rowLabels = labelsRow,
            colLabels = labelsCol,
            rowDim = "Muscle Group",
            colDim = "Day",
        )

        HeatmapChart(
            modifier = Modifier.size(300.dp),
            data = ds
        )
    }
}