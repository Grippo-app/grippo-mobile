package com.grippo.chart.area

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

private fun areaData(): AreaData = AreaData(
    points = listOf(
        AreaPoint(0f, 2.3f, "Mon"),
        AreaPoint(1f, 9.1f, "Tue"),
        AreaPoint(2f, 4.6f, "Wed"),
        AreaPoint(3f, 12.4f, "Thu"),
        AreaPoint(4f, 7.2f, "Fri"),
        AreaPoint(5f, 15.0f, "Sat"),
        AreaPoint(6f, 8.3f, "Sun"),
    )
)

private fun areaFew(): AreaData = AreaData(
    points = listOf(
        AreaPoint(0f, 4f, "Mon"),
        AreaPoint(1f, 6f, "Tue"),
        AreaPoint(2f, 3f, "Wed"),
    )
)

private fun areaMany(n: Int = 30): AreaData = AreaData(
    points = List(n) { i -> AreaPoint(i.toFloat(), (i % 7 + (i % 3) * 2).toFloat(), i.toString()) }
)

private fun areaStyle(adaptive: Boolean): AreaStyle = AreaStyle(
    grid = AreaStyle.Grid(show = true, color = Color(0x14000000), strokeWidth = 1.dp),
    yAxis = AreaStyle.YAxis.Labels(
        targetTicks = 5,
        textStyle = TextStyle(color = Color(0xFF333333)),
        formatter = { v, _ -> v.toInt().toString() },
        tickMarkColor = Color(0x14000000),
        tickMarkWidth = 1.dp
    ),
    yAxisLine = AreaStyle.AxisLine(color = Color(0x14000000), width = 1.dp),
    xAxis = if (adaptive) AreaStyle.XAxis.LabelsAdaptive(
        textStyle = TextStyle(color = Color(0xFF808080)),
        minGapDp = 1.dp
    ) else AreaStyle.XAxis.LabelsShowAll(
        textStyle = TextStyle(color = Color(0xFF808080))
    ),
    line = AreaStyle.Line(
        strokeWidth = 2.dp,
        color = Color(0xFF6AA9FF),
        brushProvider = { Brush.horizontalGradient(listOf(Color(0xFF6AA9FF), Color(0xFFB049F8))) },
        curved = true,
        curveSmoothness = 0.2f,
        clampOvershoot = true
    ),
    glow = AreaStyle.Glow(width = 8.dp, color = Color(0x336AA9FF)),
    fill = AreaStyle.Fill { sz ->
        Brush.verticalGradient(
            0f to Color(0x2A6AA9FF), 1f to Color(0x006AA9FF), startY = 0f, endY = sz.height
        )
    },
    dots = AreaStyle.Dots.Visible(radius = 2.dp, color = Color(0xFF6AA9FF)),
    extrema = AreaStyle.Extrema.Visible(
        textStyle = TextStyle(color = Color(0xFF333333)),
        markerRadius = 3.dp,
        markerColor = null
    ),
    labelPadding = 6.dp
)

@Preview
@Composable
private fun PreviewAreaAdaptive() {
    AreaChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = areaData(),
        style = areaStyle(adaptive = true)
    )
}

@Preview
@Composable
private fun PreviewAreaShowAll() {
    AreaChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = areaData(),
        style = areaStyle(adaptive = false)
    )
}

@Preview
@Composable
private fun PreviewAreaFewAdaptive() {
    AreaChart(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        data = areaFew(),
        style = areaStyle(adaptive = true)
    )
}

@Preview
@Composable
private fun PreviewAreaManyShowAll() {
    AreaChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = areaMany(40),
        style = areaStyle(adaptive = false)
    )
}



