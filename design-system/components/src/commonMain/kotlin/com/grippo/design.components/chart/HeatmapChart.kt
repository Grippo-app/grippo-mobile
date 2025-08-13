package com.grippo.design.components.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.grippo.chart.heatmap.HeatmapCell
import com.grippo.chart.heatmap.HeatmapChart
import com.grippo.chart.heatmap.HeatmapData
import com.grippo.chart.heatmap.HeatmapStyle
import com.grippo.chart.heatmap.defaultCoolWarm
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun HeatmapChart(
    modifier: Modifier = Modifier
) {
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
            textStyle = TextStyle(color = Color(0x77FFFFFF))
        ),
        legend = HeatmapStyle.Legend(
            show = true,
            height = 12.dp,
            stops = null,
            labelStyle = TextStyle(color = Color(0x66FFFFFF)),
            minText = { "0%" },
            maxText = { "100%" }),
        palette = HeatmapStyle.Palette(
            colorScale = { v -> defaultCoolWarm(v) },
            autoNormalize = false,
            missingCellColor = Color(0x11FFFFFF)
        ),
        cells = HeatmapStyle.Cells(
            showBorders = false,
            borderColor = Color(0x22FFFFFF),
            borderWidth = 1.dp
        ),
        values = HeatmapStyle.Values(
            show = false,
            textStyle = TextStyle(color = Color.White),
            formatter = { p, _ -> "${(p * 100f).roundToInt()}%" })
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
                textStyle = TextStyle(color = Color(0x77FFFFFF))
            ),
            legend = HeatmapStyle.Legend(
                show = true,
                height = 12.dp,
                stops = null,
                labelStyle = TextStyle(color = Color(0x66FFFFFF)),
                minText = { "0%" },
                maxText = { "100%" }),
            palette = HeatmapStyle.Palette(
                colorScale = { v -> defaultCoolWarm(v) },
                autoNormalize = false,
                missingCellColor = Color(0x11FFFFFF)
            ),
            cells = HeatmapStyle.Cells(
                showBorders = false,
                borderColor = Color(0x22FFFFFF),
                borderWidth = 1.dp
            ),
            values = HeatmapStyle.Values(
                show = false,
                textStyle = TextStyle(color = Color.White),
                formatter = { p, _ -> "${(p * 100f).roundToInt()}%" })
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            HeatmapChart(
                modifier = Modifier.fillMaxSize(),
                data = data,
                style = style
            )
        }
    }
}