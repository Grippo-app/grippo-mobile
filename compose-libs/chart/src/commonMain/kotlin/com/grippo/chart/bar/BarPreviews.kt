package com.grippo.chart.bar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

private val palette = listOf(
    Color(0xFF6AA9FF), Color(0xFF00E6A7), Color(0xFFFF7A33),
    Color(0xFFB049F8), Color(0xFFFFC53D), Color(0xFF3A86FF), Color(0xFFFF5E8A)
)

private fun weeklyData(): BarData = BarData(
    items = listOf(
        BarEntry("Mon", 6f, palette[0]),
        BarEntry("Tue", 10f, palette[1]),
        BarEntry("Wed", 4f, palette[2]),
        BarEntry("Thu", 12f, palette[3]),
        BarEntry("Fri", 8f, palette[4]),
        BarEntry("Sat", 14f, palette[5]),
        BarEntry("Sun", 9f, palette[6]),
    )
)

private fun fewData(): BarData = BarData(
    items = listOf(
        BarEntry("A", 5f, palette[0]),
        BarEntry("B", 12f, palette[3]),
        BarEntry("C", 7f, palette[5]),
    )
)

private fun manyData(n: Int = 20): BarData = BarData(
    items = List(n) { i ->
        val v = (i % 7) * 2 + (i % 3) * 3 + 4f
        BarEntry((i + 1).toString(), v, palette[i % palette.size])
    }
)

private fun wideRangeData(): BarData = BarData(
    items = listOf(
        BarEntry("1", 1f, palette[0]),
        BarEntry("2", 5f, palette[1]),
        BarEntry("3", 12f, palette[2]),
        BarEntry("4", 22f, palette[3]),
        BarEntry("5", 35f, palette[4]),
        BarEntry("6", 48f, palette[5]),
        BarEntry("7", 60f, palette[6]),
        BarEntry("8", 80f, palette[0]),
        BarEntry("9", 100f, palette[1]),
    )
)

private fun barStyle(
    xAdaptive: Boolean,
    valuesMode: BarStyle.Values,
    sizing: BarStyle.BarsSizing,
): BarStyle = BarStyle(
    layout = BarStyle.Layout(labelPadding = 6.dp),
    grid = BarStyle.Grid(show = true, color = Color(0x1A000000), strokeWidth = 1.dp),
    yAxis = BarStyle.YAxis.Labels(
        ticks = 5,
        textStyle = TextStyle(color = Color(0xFF333333)),
        formatter = { v, _ -> v.toInt().toString() },
        tickMarkColor = Color(0x14000000),
        tickMarkWidth = 1.dp
    ),
    yAxisLine = BarStyle.AxisLine(color = Color(0x14000000), width = 1.dp),
    xAxis = if (xAdaptive) BarStyle.XAxis.LabelsAdaptive(
        textStyle = TextStyle(color = Color(0xFF808080)),
        minGapDp = 1.dp
    ) else BarStyle.XAxis.LabelsShowAll(
        textStyle = TextStyle(color = Color(0xFF808080))
    ),
    xBaseline = BarStyle.Baseline(color = Color(0x14000000), width = 1.dp),
    bars = BarStyle.Bars(
        corner = 10.dp,
        brushProvider = { entry, _, rect ->
            Brush.verticalGradient(
                0f to entry.color.copy(alpha = 0.95f),
                1f to entry.color.copy(alpha = 0.65f),
                startY = rect.top,
                endY = rect.bottom
            )
        },
        strokeWidth = 0.dp,
        strokeColor = Color(0x14000000),
        sizing = sizing
    ),
    values = valuesMode,
    target = BarStyle.Target(value = 11f, color = Color(0x33000000), width = 1.dp),
)

@Preview
@Composable
private fun PreviewBarAdaptiveAboveAuto() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = barStyle(
            xAdaptive = true,
            valuesMode = BarStyle.Values.Above(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
        )
    )
}

@Preview
@Composable
private fun PreviewBarShowAllOutsideFixed() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = barStyle(
            xAdaptive = false,
            valuesMode = BarStyle.Values.Outside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.FixedBarWidth(18.dp)
        )
    )
}

@Preview
@Composable
private fun PreviewBarAdaptiveInsideExplicit() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = barStyle(
            xAdaptive = true,
            valuesMode = BarStyle.Values.Inside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() },
                minInnerPadding = 6.dp,
                insideColor = null
            ),
            sizing = BarStyle.BarsSizing.Explicit(width = 18.dp, spacing = 10.dp)
        )
    )
}

@Preview
@Composable
private fun PreviewBarShowAllNoneAuto() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = barStyle(
            xAdaptive = false,
            valuesMode = BarStyle.Values.None,
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
        )
    )
}

@Preview
@Composable
private fun PreviewBarAdaptiveAboveFixedWide() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = barStyle(
            xAdaptive = true,
            valuesMode = BarStyle.Values.Above(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.FixedBarWidth(24.dp)
        )
    )
}

@Preview
@Composable
private fun PreviewBarManyAdaptiveAboveAuto() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = manyData(20),
        style = barStyle(
            xAdaptive = true,
            valuesMode = BarStyle.Values.Above(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
        )
    )
}

@Preview
@Composable
private fun PreviewBarManyAdaptiveOutsideFixed() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = manyData(24),
        style = barStyle(
            xAdaptive = true,
            valuesMode = BarStyle.Values.Outside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.FixedBarWidth(14.dp)
        )
    )
}

@Preview
@Composable
private fun PreviewBarFewShowAllAboveExplicit() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        data = fewData(),
        style = barStyle(
            xAdaptive = false,
            valuesMode = BarStyle.Values.Above(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.Explicit(22.dp, 12.dp)
        )
    )
}

@Preview
@Composable
private fun PreviewBarFewShowAllInsideAuto() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        data = fewData(),
        style = barStyle(
            xAdaptive = false,
            valuesMode = BarStyle.Values.Inside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() },
                minInnerPadding = 6.dp,
                insideColor = null
            ),
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
        )
    )
}

@Preview
@Composable
private fun PreviewBarTenShowAllOutsideAuto() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = manyData(10),
        style = barStyle(
            xAdaptive = false,
            valuesMode = BarStyle.Values.Outside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
        )
    )
}

@Preview
@Composable
private fun PreviewBarWideRangeAdaptiveAboveFixed() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = wideRangeData(),
        style = barStyle(
            xAdaptive = true,
            valuesMode = BarStyle.Values.Above(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.FixedBarWidth(16.dp)
        )
    )
}

@Preview
@Composable
private fun PreviewBarNoYAxisAdaptiveAboveAuto() {
    val base = barStyle(
        xAdaptive = true,
        valuesMode = BarStyle.Values.Above(
            textStyle = TextStyle(color = Color(0xFF222222)),
            formatter = { v, _ -> v.toInt().toString() }
        ),
        sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
    )
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = base.copy(yAxis = BarStyle.YAxis.None, yAxisLine = null)
    )
}

@Preview
@Composable
private fun PreviewBarNoGridShowAllAboveAuto() {
    val base = barStyle(
        xAdaptive = false,
        valuesMode = BarStyle.Values.Above(
            textStyle = TextStyle(color = Color(0xFF222222)),
            formatter = { v, _ -> v.toInt().toString() }
        ),
        sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
    )
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = base.copy(grid = base.grid.copy(show = false))
    )
}

@Preview
@Composable
private fun PreviewBarNoBaselineAdaptiveAboveFixed() {
    val base = barStyle(
        xAdaptive = true,
        valuesMode = BarStyle.Values.Above(
            textStyle = TextStyle(color = Color(0xFF222222)),
            formatter = { v, _ -> v.toInt().toString() }
        ),
        sizing = BarStyle.BarsSizing.FixedBarWidth(18.dp)
    )
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = base.copy(xBaseline = null)
    )
}

@Preview
@Composable
private fun PreviewBarNoYAxisLineAdaptiveInsideExplicit() {
    val base = barStyle(
        xAdaptive = true,
        valuesMode = BarStyle.Values.Inside(
            textStyle = TextStyle(color = Color(0xFF222222)),
            formatter = { v, _ -> v.toInt().toString() },
            minInnerPadding = 6.dp,
            insideColor = null
        ),
        sizing = BarStyle.BarsSizing.Explicit(16.dp, 8.dp)
    )
    BarChart(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        data = weeklyData(),
        style = base.copy(yAxisLine = null)
    )
}

@Preview
@Composable
private fun PreviewBarDenseManyShowAllOutsideFixed() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        data = manyData(30),
        style = barStyle(
            xAdaptive = false,
            valuesMode = BarStyle.Values.Outside(
                textStyle = TextStyle(color = Color(0xFF222222)),
                formatter = { v, _ -> v.toInt().toString() }
            ),
            sizing = BarStyle.BarsSizing.FixedBarWidth(10.dp)
        )
    )
}

@Preview
@Composable
private fun PreviewBarFewAdaptiveNoneExplicit() {
    BarChart(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        data = fewData(),
        style = barStyle(
            xAdaptive = true,
            valuesMode = BarStyle.Values.None,
            sizing = BarStyle.BarsSizing.Explicit(24.dp, 14.dp)
        )
    )
}



