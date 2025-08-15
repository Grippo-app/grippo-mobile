package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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

@Immutable
public data class DSAreaPoint(
    val x: Float,
    val y: Float,
    val xLabel: String? = null,
)

@Immutable
public data class DSAreaData(
    val points: List<DSAreaPoint>
)

@Composable
public fun AreaChart(
    modifier: Modifier = Modifier,
    data: DSAreaData
) {
    val charts = AppTokens.colors.charts

    val style = AreaStyle(
        grid = AreaStyle.Grid(
            show = true,
            color = AppTokens.colors.divider.default,
            strokeWidth = 1.dp
        ),
        yAxis = AreaStyle.YAxis.Labels(
            targetTicks = 5,
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary),
            formatter = { v, _ -> v.roundToInt().toString() },
            tickMarkColor = AppTokens.colors.divider.default,
            tickMarkWidth = 1.dp
        ),
        yAxisLine = AreaStyle.AxisLine(
            color = AppTokens.colors.divider.default,
            width = 1.dp
        ),
        xAxis = AreaStyle.XAxis.LabelsAdaptive(
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.secondary),
            minGapDp = 1.dp
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
        dots = AreaStyle.Dots.Visible(radius = 2.dp, color = charts.area.dot),
        extrema = AreaStyle.Extrema.Visible(
            textStyle = AppTokens.typography.b11Bold().copy(color = AppTokens.colors.text.primary),
            markerColor = null,
            markerRadius = 3.dp
        )
    )

    AreaChart(
        modifier = modifier,
        data = remember(data) { data.toChart() },
        style = style
    )
}

private fun DSAreaPoint.toChart(): AreaPoint = AreaPoint(
    x = x,
    y = y,
    xLabel = xLabel
)

private fun DSAreaData.toChart(): AreaData = AreaData(
    points = points.map { it.toChart() }
)

@AppPreview
@Composable
private fun AreaChartPreview() {
    PreviewContainer {
        val ds = DSAreaData(
            points = listOf(
                DSAreaPoint(0f, 0f, "Mon"),
                DSAreaPoint(2f, 6f, "Wed"),
                DSAreaPoint(4f, 8f, "Fri"),
                DSAreaPoint(6f, 7f, "Sun"),
                DSAreaPoint(8f, 9f, "Tue"),
                DSAreaPoint(10f, 11f, "Thu"),
            )
        )

        AreaChart(
            modifier = Modifier.size(300.dp),
            data = ds
        )
    }
}