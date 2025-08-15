package com.grippo.chart.progress

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.grippo.chart.progress.*
import org.jetbrains.compose.ui.tooling.preview.Preview

private fun progressData(): ProgressData = ProgressData(
    items = listOf(
        ProgressChartData("Bench Press", 72f, Color(0xFF6AA9FF)),
        ProgressChartData("Deadlift", 100f, Color(0xFF00E6A7)),
        ProgressChartData("Squat", 86f, Color(0xFFFF7A33)),
        ProgressChartData("Overhead", 58f, Color(0xFFB049F8)),
        ProgressChartData("Row", 64f, Color(0xFFFFC53D)),
    ),
    valueUnit = "%",
)

private fun progressFew(): ProgressData = ProgressData(
    items = listOf(
        ProgressChartData("A", 25f, Color(0xFF6AA9FF)),
        ProgressChartData("B", 75f, Color(0xFF00E6A7)),
    ),
    valueUnit = "%",
)

private fun progressStyle(values: ProgressStyle.Values): ProgressStyle = ProgressStyle(
    layout = ProgressStyle.Layout(
        barHeight = 16.dp,
        spacing = 12.dp,
        corner = 10.dp,
        labelPadding = 8.dp
    ),
    domain = ProgressStyle.Domain.Absolute(maxValue = 100f),
    bars = ProgressStyle.Bars(
        trackColor = Color(0x11000000),
        brushProvider = { entry, _, rect ->
            Brush.horizontalGradient(
                0f to entry.color.copy(alpha = 0.95f), 1f to entry.color.copy(alpha = 0.70f),
                startX = rect.left, endX = rect.right
            )
        },
        strokeWidth = 0.dp,
        strokeColor = Color(0x14000000)
    ),
    labels = ProgressStyle.Labels(textStyle = TextStyle(color = Color(0xFF333333))),
    values = values,
    target = ProgressStyle.Target(value = 80f, color = Color(0x33000000), width = 1.dp)
)

@Preview
@Composable
private fun PreviewProgressInside() {
    ProgressChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = progressData(),
        style = progressStyle(
            ProgressStyle.Values.Inside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, d -> (v.toInt().toString() + (d.valueUnit ?: "")) },
                minInnerPadding = 6.dp,
                insideColor = null,
                preferNormalizedLabels = true
            )
        )
    )
}

@Preview
@Composable
private fun PreviewProgressOutside() {
    ProgressChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = progressData(),
        style = progressStyle(
            ProgressStyle.Values.Outside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, d -> (v.toInt().toString() + (d.valueUnit ?: "")) },
                preferNormalizedLabels = false
            )
        )
    )
}

@Preview
@Composable
private fun PreviewProgressFewInside() {
    ProgressChart(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        data = progressFew(),
        style = progressStyle(
            ProgressStyle.Values.Inside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, d -> (v.toInt().toString() + (d.valueUnit ?: "")) },
                minInnerPadding = 6.dp,
                insideColor = null,
                preferNormalizedLabels = true
            )
        )
    )
}



