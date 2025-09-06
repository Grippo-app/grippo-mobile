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
    val background = AppTokens.colors.background
    val charts = AppTokens.colors.charts
    val palette = AppTokens.colors.palette

    // Discrete color scale: picks exact color from the palette, no interpolation.
    fun colorScaleDiscreteOf(colors: List<Color>): (Float) -> Color = { tIn ->
        if (colors.isEmpty()) Color.Unspecified else {
            val t = tIn.coerceIn(0f, 1f)
            val idx = kotlin.math.min((t * colors.size).toInt(), colors.size - 1)
            colors[idx]
        }
    }

    // Build discrete color list once: bg + palette colors (no gradient math)
    val paletteColors: List<Color> = remember(background.screen, palette.palette5OrangeRedGrowth) {
        listOf(background.dialog) + palette.palette5OrangeRedGrowth
    }

    val discreteScale = remember(paletteColors) { colorScaleDiscreteOf(paletteColors) }

    val style = HeatmapStyle(
        layout = HeatmapStyle.Layout(
            gap = 6.dp,
            corner = 6.dp,
            labelPadding = 6.dp,
            maxCellSize = null
        ),
        rowLabels = HeatmapStyle.AxisLabels.ShowAll(
            textStyle = AppTokens.typography.b10Reg().copy(color = AppTokens.colors.text.primary)
        ),
        colLabels = HeatmapStyle.AxisLabels.Adaptive(
            textStyle = AppTokens.typography.b10Reg().copy(color = AppTokens.colors.text.primary),
            minGapDp = 1.dp
        ),
        legend = HeatmapStyle.Legend.Visible(
            height = 8.dp,
            labelStyle = AppTokens.typography.b10Reg()
                .copy(color = AppTokens.colors.text.secondary),
            minText = null,
            maxText = null
        ),
        palette = HeatmapStyle.Palette(
            colorScale = discreteScale     // <- exact palette colors only
        )
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
                ((base % 6) / 5f) // spreads values across discrete bins
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