package com.grippo.chart.sparkline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

private fun sparkStyle(curved: Boolean): SparklineStyle = SparklineStyle(
    line = SparklineStyle.Line(
        stroke = 2.dp,
        color = Color(0xFF6AA9FF),
        brush = { rect ->
            Brush.horizontalGradient(
                listOf(Color(0xFF6AA9FF), Color(0xFFB049F8)),
                rect.left,
                rect.right
            )
        },
        curved = curved,
        curveSmoothness = 0.25f,
        clampOvershoot = true
    ),
    midline = SparklineStyle.Midline.Visible(
        color = Color(0xFF6AA9FF),
        width = 1.dp,
        dash = 3.dp,
        gap = 3.dp
    ),
    fill = SparklineStyle.Fill(
        provider = { rect ->
            Brush.verticalGradient(
                0f to Color(0x2A6AA9FF),
                1f to Color(0x006AA9FF),
                startY = rect.top,
                endY = rect.bottom
            )
        }
    ),
    baseline = SparklineStyle.Baseline.None,
    dots = SparklineStyle.Dots.Visible(radius = 2.dp, color = null),
    extremes = SparklineStyle.Extremes.Visible(
        minColor = Color(0xFFFFC53D),
        maxColor = Color(0xFF00E6A7),
        radius = 3.dp
    )
)

@Preview
@Composable
private fun PreviewSparkCurved() {
    Sparkline(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        data = SparklineData(
            points = listOf(
                4f,
                6f,
                5f,
                8f,
                9f,
                7f,
                12f,
                10f,
                13f,
                11f,
                16f
            ).mapIndexed { i, v -> SparklinePoint(i.toFloat(), v) }),
        style = sparkStyle(curved = true)
    )
}

@Preview
@Composable
private fun PreviewSparkLinear() {
    Sparkline(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        data = SparklineData(
            points = listOf(
                4f,
                6f,
                5f,
                8f,
                9f,
                7f,
                12f,
                10f,
                13f,
                11f,
                16f
            ).mapIndexed { i, v -> SparklinePoint(i.toFloat(), v) }),
        style = sparkStyle(curved = false)
    )
}

@Preview
@Composable
private fun PreviewSparkFew() {
    Sparkline(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        data = SparklineData(
            points = listOf(
                4f,
                7f,
                3f
            ).mapIndexed { i, v -> SparklinePoint(i.toFloat(), v) }),
        style = sparkStyle(curved = true)
    )
}

