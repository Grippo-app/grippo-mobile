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
import com.grippo.chart.bar.BarChart
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.chart.bar.BarStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun BarChart(
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        BarEntry("Mon", 6f, Color(0xFF6AA9FF)),
        BarEntry("Tue", 10f, Color(0xFF00E6A7)),
        BarEntry("Wed", 4f, Color(0xFFFF7A33)),
        BarEntry("Thu", 12f, Color(0xFFB049F8)),
        BarEntry("Fri", 8f, Color(0xFFFFC53D)),
        BarEntry("Sat", 14f, Color(0xFF3A86FF)),
        BarEntry("Sun", 9f, Color(0xFFFF5E8A)),
    )

    val data = BarData(
        items = entries,
        xName = "Day",
        yName = "Volume",
        yUnit = null,
    )

    val style = BarStyle(
        layout = BarStyle.Layout(padding = 12.dp, labelPadding = 6.dp),
        grid = BarStyle.Grid(show = true, color = Color(0x22FFFFFF), strokeWidth = 1.dp),
        yAxis = BarStyle.YAxis(
            show = true,
            ticks = 5,
            textStyle = TextStyle(color = Color(0x77FFFFFF)),
            showLine = true,
            axisLineColor = Color(0x33FFFFFF),
            axisLineWidth = 1.dp,
            formatter = { v, _ -> v.roundToInt().toString() }
        ),
        xAxis = BarStyle.XAxis(
            show = true,
            textStyle = TextStyle(color = Color(0x66FFFFFF)),
            showBaseline = true
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
            strokeColor = Color(0x22FFFFFF)
        ),
        values = BarStyle.Values(
            show = true,
            textStyle = TextStyle(color = Color(0xCCFFFFFF)),
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

        val data = BarData(
            items = entries,
            xName = "Day",
            yName = "Volume",
            yUnit = null,
        )

        val style = BarStyle(
            layout = BarStyle.Layout(padding = 12.dp, labelPadding = 6.dp),
            grid = BarStyle.Grid(show = true, color = Color(0x22FFFFFF), strokeWidth = 1.dp),
            yAxis = BarStyle.YAxis(
                show = true,
                ticks = 5,
                textStyle = TextStyle(color = Color(0x77FFFFFF)),
                showLine = true,
                axisLineColor = Color(0x33FFFFFF),
                axisLineWidth = 1.dp,
                formatter = { v, _ -> v.roundToInt().toString() }
            ),
            xAxis = BarStyle.XAxis(
                show = true,
                textStyle = TextStyle(color = Color(0x66FFFFFF)),
                showBaseline = true
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
                strokeColor = Color(0x22FFFFFF)
            ),
            values = BarStyle.Values(
                show = true,
                textStyle = TextStyle(color = Color(0xCCFFFFFF)),
                formatter = { v, _ -> v.roundToInt().toString() },
                placement = BarStyle.ValuePlacement.Above,
                minInnerPadding = 6.dp,
                insideColor = null,
            ),
            target = null
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            BarChart(
                modifier = Modifier.fillMaxSize(),
                data = data,
                style = style
            )
        }
    }
}