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
import com.grippo.chart.area.AreaChart
import com.grippo.chart.area.AreaData
import com.grippo.chart.area.AreaPoint
import com.grippo.chart.area.AreaStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun AreaChart(
    modifier: Modifier = Modifier
) {
    val data = AreaData(
        points = listOf(
            AreaPoint(0f, 0f),
            AreaPoint(1f, 4f),
            AreaPoint(2f, 6f),
            AreaPoint(3f, 3f),
            AreaPoint(4f, 8f),
            AreaPoint(5f, 10f),
            AreaPoint(6f, 7f),
            AreaPoint(7f, 12f),
            AreaPoint(8f, 9f),
            AreaPoint(9f, 14f),
            AreaPoint(10f, 11f),
            AreaPoint(11f, 16f)
        ),
        xLabels = listOf(
            0f to "Mon",
            2f to "Wed",
            4f to "Fri",
            6f to "Sun",
            8f to "Tue",
            10f to "Thu"
        ),
        xName = "Day",
        yName = "Weight",
        yUnit = "kg",
    )

    val style = AreaStyle(
        layout = AreaStyle.Layout(padding = 12.dp, labelPadding = 6.dp),
        grid = AreaStyle.Grid(show = true, color = Color(0x22FFFFFF), strokeWidth = 1.dp),
        yAxis = AreaStyle.YAxis(
            show = true,
            ticks = 5,
            textStyle = TextStyle(color = Color(0x77FFFFFF)),
            showLine = true,
            axisLineColor = Color(0x33FFFFFF),
            axisLineWidth = 1.dp,
            formatter = { v, d -> "${v.roundToInt()} ${d.yUnit ?: ""}".trim() }
        ),
        xAxis = AreaStyle.XAxis(show = true, textStyle = TextStyle(color = Color(0x66FFFFFF))),
        line = AreaStyle.Line(
            strokeWidth = 2.dp,
            color = Color(0xFF00E6A7),
            brushProvider = { _ ->
                Brush.horizontalGradient(
                    listOf(Color(0xFF00E6A7), Color(0xFF6AA9FF))
                )
            },
            curved = true,
            curveSmoothness = 0.20f,
            clampOvershoot = true,
        ),
        glow = AreaStyle.Glow(width = 8.dp, color = null),
        fill = AreaStyle.Fill { sz ->
            Brush.verticalGradient(
                0f to Color(0xFF00E6A7).copy(alpha = 0.18f),
                1f to Color(0xFF00E6A7).copy(alpha = 0.00f),
                startY = 0f,
                endY = sz.height
            )
        },
        dots = AreaStyle.Dots(show = true, radius = 2.dp, color = null),
        extrema = AreaStyle.Extrema(show = true, textStyle = TextStyle(color = Color(0xCCFFFFFF)))
    )

    AreaChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun AreaChartPreview() {
    PreviewContainer {
        val data = AreaData(
            points = listOf(
                AreaPoint(0f, 0f),
                AreaPoint(1f, 4f),
                AreaPoint(2f, 6f),
                AreaPoint(3f, 3f),
                AreaPoint(4f, 8f),
                AreaPoint(5f, 10f),
                AreaPoint(6f, 7f),
                AreaPoint(7f, 12f),
                AreaPoint(8f, 9f),
                AreaPoint(9f, 14f),
                AreaPoint(10f, 11f),
                AreaPoint(11f, 16f)
            ),
            xLabels = listOf(
                0f to "Mon",
                2f to "Wed",
                4f to "Fri",
                6f to "Sun",
                8f to "Tue",
                10f to "Thu"
            ),
            xName = "Day",
            yName = "Weight",
            yUnit = "kg",
        )

        val style = AreaStyle(
            layout = AreaStyle.Layout(padding = 12.dp, labelPadding = 6.dp),
            grid = AreaStyle.Grid(show = true, color = Color(0x22FFFFFF), strokeWidth = 1.dp),
            yAxis = AreaStyle.YAxis(
                show = true,
                ticks = 5,
                textStyle = TextStyle(color = Color(0x77FFFFFF)),
                showLine = true,
                axisLineColor = Color(0x33FFFFFF),
                axisLineWidth = 1.dp,
                formatter = { v, d -> "${v.roundToInt()} ${d.yUnit ?: ""}".trim() }
            ),
            xAxis = AreaStyle.XAxis(show = true, textStyle = TextStyle(color = Color(0x66FFFFFF))),
            line = AreaStyle.Line(
                strokeWidth = 2.dp,
                color = Color(0xFF00E6A7),
                brushProvider = { _ ->
                    Brush.horizontalGradient(listOf(Color(0xFF00E6A7), Color(0xFF6AA9FF)))
                },
                curved = true,
                curveSmoothness = 0.20f,
                clampOvershoot = true,
            ),
            glow = AreaStyle.Glow(width = 8.dp, color = null),
            fill = AreaStyle.Fill { sz ->
                Brush.verticalGradient(
                    0f to Color(0xFF00E6A7).copy(alpha = 0.18f),
                    1f to Color(0xFF00E6A7).copy(alpha = 0.00f),
                    startY = 0f,
                    endY = sz.height
                )
            },
            dots = AreaStyle.Dots(show = true, radius = 2.dp, color = null),
            extrema = AreaStyle.Extrema(
                show = true,
                textStyle = TextStyle(color = Color(0xCCFFFFFF))
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            AreaChart(
                modifier = Modifier.fillMaxSize(),
                data = data,
                style = style
            )
        }
    }
}