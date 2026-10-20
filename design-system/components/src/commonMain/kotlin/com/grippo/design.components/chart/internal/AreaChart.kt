package com.grippo.design.components.chart.internal

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
internal fun AreaChart(
    modifier: Modifier = Modifier,
    data: AreaData
) {
    val charts = AppTokens.colors.charts

    val style = AreaStyle(
        grid = AreaStyle.Grid(
            show = true,
            color = AppTokens.colors.divider.subtle,
            strokeWidth = AppTokens.dp.metrics.charts.axis.strokeWidth
        ),
        yAxis = AreaStyle.YAxis.Labels(
            targetTicks = 5,
            textStyle = AppTokens.typography.b10Med().copy(color = AppTokens.colors.text.secondary),
            formatter = { v, _ -> v.roundToInt().toString() },
            tickMarkColor = AppTokens.colors.divider.subtle,
            tickMarkWidth = AppTokens.dp.metrics.charts.axis.strokeWidth
        ),
        yAxisLine = null,
        xAxis = AreaStyle.XAxis.LabelsAdaptive(
            textStyle = AppTokens.typography.b10Med().copy(color = AppTokens.colors.text.secondary),
            minGapDp = AppTokens.dp.metrics.charts.area.axisLabelGap
        ),
        line = AreaStyle.Line(
            strokeWidth = AppTokens.dp.metrics.charts.area.lineWidth,
            color = charts.area.lineA,
            brushProvider = null,
            curved = true,
            curveSmoothness = 0.20f,
            clampOvershoot = true
        ),
        glow = AreaStyle.Glow(width = 0.dp, color = null),
        fill = AreaStyle.Fill { sz ->
            Brush.verticalGradient(
                0f to charts.area.fillStart,
                1f to charts.area.fillEnd,
                startY = 0f, endY = sz.height
            )
        },
        dots = AreaStyle.Dots.None,
        extrema = AreaStyle.Extrema.None,
        labelPadding = AppTokens.dp.metrics.charts.area.axisLabelGap,
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
        val ds = AreaData(
            points = listOf(
                AreaPoint(0f, 0f, "Mon"),
                AreaPoint(2f, 6f, "Wed"),
                AreaPoint(4f, 8f, "Fri"),
                AreaPoint(6f, 7f, "Sun"),
                AreaPoint(8f, 9f, "Tue"),
                AreaPoint(10f, 11f, "Thu"),
            )
        )

        AreaChart(
            modifier = Modifier.size(300.dp),
            data = ds
        )
    }
}

@AppPreview
@Composable
private fun AreaChartEmptyPreview() {
    PreviewContainer {
        AreaChart(
            modifier = Modifier.size(300.dp),
            data = AreaData(points = emptyList()),
        )
    }
}

@AppPreview
@Composable
private fun AreaChartSinglePointPreview() {
    PreviewContainer {
        AreaChart(
            modifier = Modifier.size(300.dp),
            data = AreaData(points = listOf(AreaPoint(0f, 82f, "20 Oct"))),
        )
    }
}

@AppPreview
@Composable
private fun AreaChartFlatPreview() {
    PreviewContainer {
        AreaChart(
            modifier = Modifier.size(300.dp),
            data = AreaData(points = List(7) { day -> AreaPoint(day.toFloat(), 82f, "${day + 1} Oct") }),
        )
    }
}

@AppPreview
@Composable
private fun AreaChartDensePreview() {
    PreviewContainer {
        AreaChart(
            modifier = Modifier.size(300.dp),
            data = AreaData(
                points = List(31) { day ->
                    AreaPoint(day.toFloat(), 80f + day % 5, "${day + 1} October 2026")
                },
            ),
        )
    }
}
