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
import com.grippo.chart.progress.ProgressChart
import com.grippo.chart.progress.ProgressChartData
import com.grippo.chart.progress.ProgressStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun ProgressChart(
    modifier: Modifier = Modifier
) {
    val data = listOf(
        ProgressChartData("Bench Press", 72f, Color(0xFF6AA9FF)),
        ProgressChartData("Deadlift", 100f, Color(0xFF00E6A7)),
        ProgressChartData("Squat", 86f, Color(0xFFFF7A33)),
        ProgressChartData("Overhead Press", 58f, Color(0xFFB049F8)),
        ProgressChartData("Row", 64f, Color(0xFFFFC53D)),
    )

    val style = ProgressStyle(
        padding = 12.dp,
        barHeight = 16.dp,
        spacing = 12.dp,
        corner = 10.dp,
        labelPadding = 8.dp,
        normalized = false,
        maxValue = 100f,
        trackColor = Color(0x14FFFFFF),
        barBrush = { entry, _, rect ->
            Brush.horizontalGradient(
                0f to entry.color.copy(alpha = 0.95f),
                1f to entry.color.copy(alpha = 0.70f),
                startX = rect.left,
                endX = rect.right
            )
        },
        barStrokeWidth = 0.dp,
        barStrokeColor = Color(0x22FFFFFF),
        labelTextStyle = TextStyle(color = Color(0x77FFFFFF)),
        showValue = true,
        valueTextStyle = TextStyle(color = Color(0xCCFFFFFF)),
        valueFormatter = { v -> "${v.roundToInt()}%" },
        placeValueInside = true,
        minInnerPadding = 6.dp,
        valueInsideColor = null,
        targetValue = 80f,
        targetColor = Color(0x44FFFFFF),
        targetWidth = 1.dp,
    )

    ProgressChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun ProgressChartPreview() {
    PreviewContainer {
        val data = listOf(
            ProgressChartData("Bench Press", 72f, Color(0xFF6AA9FF)),
            ProgressChartData("Deadlift", 100f, Color(0xFF00E6A7)),
            ProgressChartData("Squat", 86f, Color(0xFFFF7A33)),
            ProgressChartData("Overhead Press", 58f, Color(0xFFB049F8)),
            ProgressChartData("Row", 64f, Color(0xFFFFC53D)),
        )

        val style = ProgressStyle(
            padding = 12.dp,
            barHeight = 16.dp,
            spacing = 12.dp,
            corner = 10.dp,
            labelPadding = 8.dp,
            normalized = false,
            maxValue = 100f,
            trackColor = Color(0x14FFFFFF),
            barBrush = { entry, _, rect ->
                Brush.horizontalGradient(
                    0f to entry.color.copy(alpha = 0.95f),
                    1f to entry.color.copy(alpha = 0.70f),
                    startX = rect.left,
                    endX = rect.right
                )
            },
            barStrokeWidth = 0.dp,
            barStrokeColor = Color(0x22FFFFFF),
            labelTextStyle = TextStyle(color = Color(0x77FFFFFF)),
            showValue = true,
            valueTextStyle = TextStyle(color = Color(0xCCFFFFFF)),
            valueFormatter = { v -> "${v.roundToInt()}%" },
            placeValueInside = true,
            minInnerPadding = 6.dp,
            valueInsideColor = null,
            targetValue = 80f,
            targetColor = Color(0x44FFFFFF),
            targetWidth = 1.dp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            ProgressChart(
                modifier = Modifier.fillMaxSize(),
                data = data,
                style = style
            )
        }
    }
}
