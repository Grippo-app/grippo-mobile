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
import com.grippo.chart.progress.ProgressData
import com.grippo.chart.progress.ProgressStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun ProgressChart(
    modifier: Modifier = Modifier
) {
    val data = ProgressData(
        items = listOf(
            ProgressChartData("Bench Press", 72f, Color(0xFF6AA9FF)),
            ProgressChartData("Deadlift", 100f, Color(0xFF00E6A7)),
            ProgressChartData("Squat", 86f, Color(0xFFFF7A33)),
            ProgressChartData("Overhead Press", 58f, Color(0xFFB049F8)),
            ProgressChartData("Row", 64f, Color(0xFFFFC53D)),
        ),
        valueUnit = "%",
        title = "Strength Progress",
    )

    val style = ProgressStyle(
        layout = ProgressStyle.Layout(
            padding = 12.dp,
            barHeight = 16.dp,
            spacing = 12.dp,
            corner = 10.dp,
            labelPadding = 8.dp,
        ),
        domain = ProgressStyle.Domain(
            normalized = false,
            maxValue = 100f,
        ),
        bars = ProgressStyle.Bars(
            trackColor = Color(0x14FFFFFF),
            brushProvider = { entry, _, rect ->
                Brush.horizontalGradient(
                    0f to entry.color.copy(alpha = 0.95f),
                    1f to entry.color.copy(alpha = 0.70f),
                    startX = rect.left,
                    endX = rect.right
                )
            },
            strokeWidth = 0.dp,
            strokeColor = Color(0x22FFFFFF),
        ),
        labels = ProgressStyle.Labels(
            textStyle = TextStyle(color = Color(0x77FFFFFF))
        ),
        values = ProgressStyle.Values(
            show = true,
            textStyle = TextStyle(color = Color(0xCCFFFFFF)),
            formatter = { v, d -> "${v.roundToInt()}${d.valueUnit ?: ""}" },
            placeInside = true,
            minInnerPadding = 6.dp,
            insideColor = null,
            preferNormalizedLabels = true,
        ),
        target = ProgressStyle.Target(
            value = 80f,
            color = Color(0x44FFFFFF),
            width = 1.dp,
        )
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
        val data = ProgressData(
            items = listOf(
                ProgressChartData("Bench Press", 72f, Color(0xFF6AA9FF)),
                ProgressChartData("Deadlift", 100f, Color(0xFF00E6A7)),
                ProgressChartData("Squat", 86f, Color(0xFFFF7A33)),
                ProgressChartData("Overhead Press", 58f, Color(0xFFB049F8)),
                ProgressChartData("Row", 64f, Color(0xFFFFC53D)),
            ),
            valueUnit = "%",
            title = "Strength Progress",
        )

        val style = ProgressStyle(
            layout = ProgressStyle.Layout(
                padding = 12.dp,
                barHeight = 16.dp,
                spacing = 12.dp,
                corner = 10.dp,
                labelPadding = 8.dp,
            ),
            domain = ProgressStyle.Domain(
                normalized = false,
                maxValue = 100f,
            ),
            bars = ProgressStyle.Bars(
                trackColor = Color(0x14FFFFFF),
                brushProvider = { entry, _, rect ->
                    Brush.horizontalGradient(
                        0f to entry.color.copy(alpha = 0.95f),
                        1f to entry.color.copy(alpha = 0.70f),
                        startX = rect.left,
                        endX = rect.right
                    )
                },
                strokeWidth = 0.dp,
                strokeColor = Color(0x22FFFFFF),
            ),
            labels = ProgressStyle.Labels(
                textStyle = TextStyle(color = Color(0x77FFFFFF))
            ),
            values = ProgressStyle.Values(
                show = true,
                textStyle = TextStyle(color = Color(0xCCFFFFFF)),
                formatter = { v, d -> "${v.roundToInt()}${d.valueUnit ?: ""}" },
                placeInside = true,
                minInnerPadding = 6.dp,
                insideColor = null,
                preferNormalizedLabels = true,
            ),
            target = ProgressStyle.Target(
                value = 80f,
                color = Color(0x44FFFFFF),
                width = 1.dp,
            )
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