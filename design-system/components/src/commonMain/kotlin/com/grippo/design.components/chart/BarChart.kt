package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
public fun BarChart(
    modifier: Modifier = Modifier
) {
    val charts = AppTokens.colors.charts
    val palette = charts.categorical.palette

    val entries = listOf(
        "Mon" to 6f,
        "Tue" to 10f,
        "Wed" to 4f,
        "Thu" to 12f,
        "Fri" to 8f,
        "Sat" to 14f,
        "Sun" to 9f,
    ).mapIndexed { i, (label, value) ->
        BarEntry(
            label = label,
            value = value,
            color = palette[i % palette.size]
        )
    }

    val data = BarData(
        items = entries,
        xName = "Day",
        yName = "Volume",
        yUnit = null,
    )

    val style = BarStyle(
        layout = BarStyle.Layout(
            padding = 12.dp,
            labelPadding = 6.dp
        ),
        grid = BarStyle.Grid(
            show = true,
            color = charts.surface.grid,
            strokeWidth = 1.dp
        ),
        yAxis = BarStyle.YAxis(
            show = true,
            ticks = 5,
            textStyle = TextStyle(color = charts.surface.labelPrimary),
            showLine = true,
            axisLineColor = charts.surface.axis,
            axisLineWidth = 1.dp,
            formatter = { v, _ -> v.roundToInt().toString() }
        ),
        xAxis = BarStyle.XAxis(
            show = true,
            textStyle = TextStyle(color = charts.surface.labelSecondary),
            showBaseline = true,
            baselineColor = charts.surface.axis,
            baselineWidth = 3.dp
        ),
        bars = BarStyle.Bars(
            width = 18.dp,
            spacing = 10.dp,
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
            strokeColor = charts.bar.stroke
        ),
        values = BarStyle.Values(
            show = true,
            textStyle = TextStyle(color = charts.surface.extremaLabel),
            formatter = { v, _ -> v.roundToInt().toString() },
            placement = BarStyle.ValuePlacement.Above,
            minInnerPadding = 6.dp,
            insideColor = null,
        ),
        target = null
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
        val entries = listOf(
            BarEntry("Mon", 6f, Color(0xFF6AA9FF)),
            BarEntry("Tue", 10f, Color(0xFF00E6A7)),
            BarEntry("Wed", 4f, Color(0xFFFF7A33)),
            BarEntry("Thu", 12f, Color(0xFFB049F8)),
            BarEntry("Fri", 8f, Color(0xFFFFC53D)),
            BarEntry("Sat", 14f, Color(0xFF3A86FF)),
            BarEntry("Sun", 9f, Color(0xFFFF5E8A)),
        )

        BarData(
            items = entries,
            xName = "Day",
            yName = "Volume",
            yUnit = null,
        )

        BarChart(
            modifier = Modifier.size(300.dp)
        )
    }
}