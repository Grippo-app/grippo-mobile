package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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

@Immutable
public data class DSBarItem(
    val label: String,
    val value: Float,
    val color: Color,
)

@Immutable
public data class DSBarData(
    val items: List<DSBarItem>,
    val xName: String? = null,
    val yName: String? = null,
    val yUnit: String? = null,
)

@Immutable
public enum class XAxisLabelStyle {
    ADAPTIVE,
    SHOW_ALL,
}

@Composable
public fun BarChart(
    modifier: Modifier = Modifier,
    data: DSBarData,
    xAxisLabelStyle: XAxisLabelStyle
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
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary),
            formatter = { v, _ -> v.roundToInt().toString() },
            tickMarkColor = AppTokens.colors.divider.default,
            tickMarkWidth = 1.dp
        ),
        yAxisLine = BarStyle.AxisLine(
            color = AppTokens.colors.divider.default,
            width = 1.dp
        ),
        xAxis = when (xAxisLabelStyle) {
            XAxisLabelStyle.ADAPTIVE -> BarStyle.XAxis.LabelsAdaptive(
                textStyle = AppTokens.typography.b11Reg()
                    .copy(color = AppTokens.colors.text.secondary),
                minGapDp = 1.dp
            )

            XAxisLabelStyle.SHOW_ALL -> BarStyle.XAxis.LabelsShowAll(
                textStyle = AppTokens.typography.b11Reg()
                    .copy(color = AppTokens.colors.text.secondary),
            )
        },
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
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps
        ),
        values = BarStyle.Values.Above(
            textStyle = AppTokens.typography.b11Bold().copy(color = AppTokens.colors.text.primary),
            formatter = { v, _ -> v.roundToInt().toString() },
        ),
        target = null
    )

    BarChart(
        modifier = modifier,
        data = remember(data) { data.toChart() },
        style = style
    )
}

private fun DSBarItem.toChart(): BarEntry = BarEntry(
    label = label,
    value = value,
    color = color
)

private fun DSBarData.toChart(): BarData = BarData(
    items = items.map { it.toChart() },
    xName = xName,
    yName = yName,
    yUnit = yUnit
)

@AppPreview
@Composable
private fun BarChartPreview() {
    PreviewContainer {
        val ds = DSBarData(
            items = listOf(
                DSBarItem("Mon", 6f, Color(0xFF6AA9FF)),
                DSBarItem("Tue", 10f, Color(0xFF00E6A7)),
                DSBarItem("Wed", 4f, Color(0xFFFF7A33)),
                DSBarItem("Thu", 12f, Color(0xFFB049F8)),
                DSBarItem("Fri", 8f, Color(0xFFFFC53D)),
                DSBarItem("Sat", 14f, Color(0xFF3A86FF)),
                DSBarItem("Sun", 9f, Color(0xFFFF5E8A)),
            ),
            xName = "Day",
            yName = "Volume",
        )

        BarChart(
            modifier = Modifier.size(300.dp),
            data = ds,
            xAxisLabelStyle = XAxisLabelStyle.ADAPTIVE
        )
    }
}