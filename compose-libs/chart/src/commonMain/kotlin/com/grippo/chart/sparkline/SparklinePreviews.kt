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
                colors = listOf(Color(0xFF6AA9FF), Color(0xFFB049F8)),
                startX = rect.left,
                endX = rect.right
            )
        },
        curved = curved,
        curveSmoothness = 0.25f,
        clampOvershoot = true
    ),
    midline = SparklineStyle.Midline.Visible(
        position = 0.5f,
        color = Color(0xFF6AA9FF).copy(alpha = 0.25f),
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

    peek = SparklineStyle.Peek.Visible(
        hitSlop = 18.dp,

        guideColor = Color(0xFFFFFFFF).copy(alpha = 0.22f),
        guideWidth = 1.dp,
        guideDash = 6.dp,
        guideGap = 6.dp,

        focusColor = null,
        focusRadius = 3.5.dp,
        focusRingWidth = 2.dp,
        focusHaloRadius = 18.dp,

        tooltipBackground = Color(0xFF12151B).copy(alpha = 0.92f),
        tooltipBorder = Color(0xFFFFFFFF).copy(alpha = 0.16f),
        tooltipText = Color(0xFFFFFFFF),
        tooltipCornerRadius = 10.dp,
        tooltipPaddingH = 10.dp,
        tooltipPaddingV = 6.dp,
        tooltipMargin = 10.dp,

        decimals = 0,
        showLabel = true
    ),

    dots = SparklineStyle.Dots.Visible(radius = 2.dp, color = null),
    extremes = SparklineStyle.Extremes.Visible(
        minColor = Color(0xFFFFC53D),
        maxColor = Color(0xFF00E6A7),
        radius = 3.dp
    )
)

private fun toPoints(values: List<Float>): List<SparklinePoint> {
    return values.mapIndexed { i, v ->
        SparklinePoint(
            x = i.toFloat(),
            y = v,
            label = "Day ${i + 1}"
        )
    }
}

@Preview
@Composable
private fun PreviewSparkCurved_WithGhostAndPeek() {
    val current = toPoints(listOf(4f, 6f, 5f, 8f, 9f, 7f, 12f, 10f, 13f, 11f, 16f))

    Sparkline(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        data = SparklineData(
            points = current,
        ),
        style = sparkStyle(curved = true)
    )
}

@Preview
@Composable
private fun PreviewSparkLinear_WithGhostAndPeek() {
    val current = toPoints(listOf(4f, 6f, 5f, 8f, 9f, 7f, 12f, 10f, 13f, 11f, 16f))

    Sparkline(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        data = SparklineData(
            points = current,
        ),
        style = sparkStyle(curved = false)
    )
}

@Preview
@Composable
private fun PreviewSparkFew_WithGhostAndPeek() {
    val current = toPoints(listOf(4f, 7f, 3f))

    Sparkline(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        data = SparklineData(
            points = current,
        ),
        style = sparkStyle(curved = true)
    )
}

@Preview
@Composable
private fun PreviewSparkSinglePoint_WithPeek() {
    val current = listOf(
        SparklinePoint(x = 0f, y = 8f, label = "Today")
    )

    Sparkline(
        modifier = Modifier.fillMaxWidth().height(88.dp),
        data = SparklineData(
            points = current,
        ),
        style = sparkStyle(curved = true)
    )
}
