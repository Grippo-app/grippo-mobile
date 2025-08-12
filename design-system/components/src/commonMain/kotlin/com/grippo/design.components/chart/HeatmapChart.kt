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

    // Sample values 0..1 (higher = hotter)
    val cells = buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val base = (r + 1) * (c + 1)
                val v = ((base % 10) / 10f).coerceIn(0f, 1f)
                add(HeatmapCell(r, c, v))
            }
        }
    }

    val style = HeatmapStyle(
        rows = rows,
        cols = cols,
        padding = 12.dp,
        gap = 6.dp,
        corner = 6.dp,
        labelPadding = 6.dp,
        rowLabels = labelsRow,
        colLabels = labelsCol,
        labelStyle = TextStyle(color = Color(0x77FFFFFF)),
        showLegend = true,
        legendHeight = 12.dp,
        legendStops = null,
        legendLabelStyle = TextStyle(color = Color(0x66FFFFFF)),
        legendMinText = { _ -> "0%" },
        legendMaxText = { _ -> "100%" },
        colorScale = { v -> defaultCoolWarm(v) },
        autoNormalize = false,
        missingCellColor = Color(0x11FFFFFF),
        showCellBorders = false,
        borderColor = Color(0x22FFFFFF),
        borderWidth = 1.dp,
        showValues = false,
        valueTextStyle = TextStyle(color = Color.White),
        valueFormatter = { p -> "${(p * 100f).roundToInt()}%" },
    )

    HeatmapChart(
        modifier = modifier,
        cells = cells,
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

        // Sample values 0..1 (higher = hotter)
        val cells = buildList {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val base = (r + 1) * (c + 1)
                    val v = ((base % 10) / 10f).coerceIn(0f, 1f)
                    add(HeatmapCell(r, c, v))
                }
            }
        }

        val style = HeatmapStyle(
            rows = rows,
            cols = cols,
            padding = 12.dp,
            gap = 6.dp,
            corner = 6.dp,
            labelPadding = 6.dp,
            rowLabels = labelsRow,
            colLabels = labelsCol,
            labelStyle = TextStyle(color = Color(0x77FFFFFF)),
            showLegend = true,
            legendHeight = 12.dp,
            legendStops = null,
            legendLabelStyle = TextStyle(color = Color(0x66FFFFFF)),
            legendMinText = { _ -> "0%" },
            legendMaxText = { _ -> "100%" },
            colorScale = { v -> defaultCoolWarm(v) },
            autoNormalize = false,
            missingCellColor = Color(0x11FFFFFF),
            showCellBorders = false,
            borderColor = Color(0x22FFFFFF),
            borderWidth = 1.dp,
            showValues = false,
            valueTextStyle = TextStyle(color = Color.White),
            valueFormatter = { p -> "${(p * 100f).roundToInt()}%" },
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
                cells = cells,
                style = style
            )
        }
    }
}
