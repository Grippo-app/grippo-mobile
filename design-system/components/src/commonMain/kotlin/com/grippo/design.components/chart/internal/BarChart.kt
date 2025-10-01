package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.bar.BarChart
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.chart.bar.BarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
internal fun BarChart(
    modifier: Modifier = Modifier,
    data: BarData,
) {
    val style = BarStyle(
        layout = BarStyle.Layout(
            labelPadding = 6.dp
        ),
        grid = BarStyle.Grid(
            show = true,
            color = AppTokens.colors.divider.default,
            strokeWidth = 1.dp
        ),
        yAxis = BarStyle.YAxis.Labels(
            ticks = 5,
            textStyle = AppTokens.typography.b10Reg().copy(color = AppTokens.colors.text.primary),
            formatter = { v, _ -> v.roundToInt().toString() },
            tickMarkColor = AppTokens.colors.divider.default,
            tickMarkWidth = 1.dp
        ),
        yAxisLine = BarStyle.AxisLine(
            color = AppTokens.colors.divider.default,
            width = 1.dp
        ),
        xAxis = BarStyle.XAxis.LabelsAdaptive(
            textStyle = AppTokens.typography.b10Reg()
                .copy(color = AppTokens.colors.text.secondary),
            minGapDp = 1.dp
        ),
        xBaseline = BarStyle.Baseline(
            color = AppTokens.colors.divider.default,
            width = 3.dp
        ),
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
            strokeColor = AppTokens.colors.divider.default,
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps()
        ),
        values = BarStyle.Values.Above(
            textStyle = AppTokens.typography.b10Bold().copy(color = AppTokens.colors.text.primary),
            formatter = { v, _ -> v.roundToInt().toString() },
        ),
    )

    BarChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun BarChartPreview() {
    PreviewContainer {
        val ds = BarData(
            items = listOf(
                BarEntry("Mon", 6f, Color(0xFF6AA9FF)),
                BarEntry("Tue", 10f, Color(0xFF00E6A7)),
                BarEntry("Wed", 4f, Color(0xFFFF7A33)),
                BarEntry("Thu", 12f, Color(0xFFB049F8)),
                BarEntry("Fri", 8f, Color(0xFFFFC53D)),
                BarEntry("Sat", 14f, Color(0xFF3A86FF)),
                BarEntry("Sun", 9f, Color(0xFFFF5E8A)),
                BarEntry("Sun", 9f, Color(0xFFFF5E8A)),
                BarEntry("Sun", 9f, Color(0xFFFF5E8A)),
                BarEntry("Sun", 9f, Color(0xFFFF5E8A)),
                BarEntry("Sun", 9f, Color(0xFFFF5E8A)),
            ),
            xName = "Day",
            yName = "Volume",
        )

        BarChart(
            modifier = Modifier.size(300.dp),
            data = ds,
        )
    }
}