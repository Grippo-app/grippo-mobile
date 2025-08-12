package com.grippo.design.components.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.grippo.chart.area.AreaChart
import com.grippo.chart.area.AreaPoint
import com.grippo.chart.area.AreaStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun AreaChart(
    modifier: Modifier = Modifier
) {
    val data = listOf(
        AreaPoint(0f, 0f),
        AreaPoint(1f, 4f),
        AreaPoint(2f, 6f),
        AreaPoint(3f, 3f),
        AreaPoint(4f, 8f),
        AreaPoint(5f, 10f),
        AreaPoint(6f, 7f),
        AreaPoint(7f, 12f),
        AreaPoint(8f, 9f),
        AreaPoint(9f, 14f),
        AreaPoint(10f, 11f),
        AreaPoint(11f, 16f)
    )

    val style = AreaStyle(
        strokeWidth = 2.dp,
        lineColor = Color(0xFF00E6A7),
        lineBrush = { sz ->
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF00E6A7),
                    Color(0xFF6AA9FF)
                )
            )
        },
        curved = true,
        curveSmoothness = 0.20f,
        clampOvershoot = true,
        fill = { sz ->
            Brush.verticalGradient(
                0f to Color(0xFF00E6A7).copy(alpha = 0.18f),
                1f to Color(0xFF00E6A7).copy(alpha = 0.00f),
                startY = 0f,
                endY = sz.height
            )
        },
        showGrid = true,
        gridColor = Color(0x22FFFFFF),
        gridStrokeWidth = 1.dp,
        showYAxis = true,
        yAxisTicks = 5,
        yAxisTextStyle = TextStyle(color = Color(0x77FFFFFF)),
        yValueFormatter = { v -> "${v.roundToInt()} kg" },
        showYAxisLine = true,
        axisLineColor = Color(0x33FFFFFF),
        axisLineWidth = 1.dp,
        showXAxis = true,
        xAxisTextStyle = TextStyle(color = Color(0x66FFFFFF)),
        xLabels = listOf(
            0f to "Mon",
            2f to "Wed",
            4f to "Fri",
            6f to "Sun",
            8f to "Tue",
            10f to "Thu"
        ),
        padding = 12.dp,
        labelPadding = 6.dp,
        showDots = true,
        dotRadius = 2.dp,
        dotColor = null,
        showExtrema = true,
        extremaTextStyle = TextStyle(color = Color(0xCCFFFFFF)),
        lineGlowWidth = 8.dp,
        lineGlowColor = null,
    )

    AreaChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun AreaChartPreview() {
    PreviewContainer {
        val data = listOf(
            AreaPoint(0f, 0f),
            AreaPoint(1f, 4f),
            AreaPoint(2f, 6f),
            AreaPoint(3f, 3f),
            AreaPoint(4f, 8f),
            AreaPoint(5f, 10f),
            AreaPoint(6f, 7f),
            AreaPoint(7f, 12f),
            AreaPoint(8f, 9f),
            AreaPoint(9f, 14f),
            AreaPoint(10f, 11f),
            AreaPoint(11f, 16f)
        )

        val style = AreaStyle(
            strokeWidth = 2.dp,
            lineColor = Color(0xFF00E6A7),
            lineBrush = { sz ->
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00E6A7),
                        Color(0xFF6AA9FF)
                    )
                )
            },
            curved = true,
            curveSmoothness = 0.20f,
            clampOvershoot = true,
            fill = { sz ->
                Brush.verticalGradient(
                    0f to Color(0xFF00E6A7).copy(alpha = 0.18f),
                    1f to Color(0xFF00E6A7).copy(alpha = 0.00f),
                    startY = 0f,
                    endY = sz.height
                )
            },
            showGrid = true,
            gridColor = Color(0x22FFFFFF),
            gridStrokeWidth = 1.dp,
            showYAxis = true,
            yAxisTicks = 5,
            yAxisTextStyle = TextStyle(color = Color(0x77FFFFFF)),
            yValueFormatter = { v -> "${v.roundToInt()} kg" },
            showYAxisLine = true,
            axisLineColor = Color(0x33FFFFFF),
            axisLineWidth = 1.dp,
            showXAxis = true,
            xAxisTextStyle = TextStyle(color = Color(0x66FFFFFF)),
            xLabels = listOf(
                0f to "Mon",
                2f to "Wed",
                4f to "Fri",
                6f to "Sun",
                8f to "Tue",
                10f to "Thu"
            ),
            padding = 12.dp,
            labelPadding = 6.dp,
            showDots = true,
            dotRadius = 2.dp,
            dotColor = null,
            showExtrema = true,
            extremaTextStyle = TextStyle(color = Color(0xCCFFFFFF)),
            lineGlowWidth = 8.dp,
            lineGlowColor = null,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            AreaChart(
                modifier = Modifier.fillMaxSize(),
                data = data,
                style = style
            )
        }
    }
}