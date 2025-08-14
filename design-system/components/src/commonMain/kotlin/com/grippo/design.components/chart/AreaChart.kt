package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.grippo.chart.area.AreaChart
import com.grippo.chart.area.AreaData
import com.grippo.chart.area.AreaPoint
import com.grippo.chart.area.AreaStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun AreaChart(
    modifier: Modifier = Modifier
) {
    val charts = AppTokens.colors.charts
    val data = AreaData(
        points = listOf(
            AreaPoint(0f, 2.3f, "Mon"),
            AreaPoint(1f, 9.1f, "Tue"),
            AreaPoint(2f, 4.6f, "Wed"),
            AreaPoint(3f, 12.4f, "Thu"),
            AreaPoint(4f, 7.2f, "Fri"),
            AreaPoint(5f, 15.0f, "Sat"),
            AreaPoint(6f, 8.3f, "Sun"),
        ),
    )

    val style = AreaStyle(
        grid = AreaStyle.Grid(
            show = true,
            color = AppTokens.colors.divider.default,
            strokeWidth = 1.dp
        ),
        yAxis = AreaStyle.YAxis(
            show = true,
            targetTicks = 5,
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary),
            showLine = true,
            axisLineColor = AppTokens.colors.divider.default,
            axisLineWidth = 1.dp,
            formatter = { v, d -> "${v.roundToInt()}" }
        ),
        xAxis = AreaStyle.XAxis(
            show = true,
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.secondary),
            minGapDp = 1.dp,
            showAll = false
        ),
        line = AreaStyle.Line(
            strokeWidth = 2.dp,
            color = charts.area.lineA,
            brushProvider = {
                Brush.horizontalGradient(
                    listOf(
                        charts.area.lineA,
                        charts.area.lineB
                    )
                )
            },
            curved = true,
            curveSmoothness = 0.20f,
            clampOvershoot = true
        ),
        glow = AreaStyle.Glow(width = 8.dp, color = charts.area.glow),
        fill = AreaStyle.Fill { sz ->
            Brush.verticalGradient(
                0f to charts.area.fillBase.copy(alpha = 0.18f),
                1f to charts.area.fillBase.copy(alpha = 0.00f),
                startY = 0f, endY = sz.height
            )
        },
        dots = AreaStyle.Dots(show = true, radius = 2.dp, color = charts.area.dot),
        extrema = AreaStyle.Extrema(
            show = true,
            textStyle = AppTokens.typography.b11Bold().copy(color = AppTokens.colors.text.primary),
            markerColor = null,
            markerRadius = 3.dp
        )
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
        AreaData(
            points = listOf(
                AreaPoint(0f, 0f, "Mon"),
                AreaPoint(2f, 6f, "Wed"),
                AreaPoint(4f, 8f, "Fri"),
                AreaPoint(6f, 7f, "Sun"),
                AreaPoint(8f, 9f, "Tue"),
                AreaPoint(10f, 11f, "Thu"),
            ),
        )

        AreaChart(
            modifier = Modifier.size(300.dp)
        )
    }
}