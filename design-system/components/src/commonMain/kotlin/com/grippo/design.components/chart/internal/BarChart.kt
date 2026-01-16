package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.grippo.chart.bar.BarChart
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.chart.bar.BarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun BarChart(
    modifier: Modifier = Modifier,
    data: BarData,
) {
    val colors = AppTokens.colors
    val typography = AppTokens.typography

    val formatter: (Float, BarData) -> String = { value, _ -> value.prettyFormat() }

    val style = BarStyle(
        layout = BarStyle.Layout(
            labelPadding = 8.dp,
            chartPadding = BarStyle.ChartPadding(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = 0.dp
            ),
            minBarHeight = 6.dp,
            yAxisLabelSpacing = 2.dp,
            valueLabelSpacing = 8.dp,
            baselineSpacing = 8.dp,
            barsAxisInsetStart = 8.dp,
            barsAxisInsetEnd = 8.dp
        ),
        grid = BarStyle.Grid(
            show = true,
            color = colors.divider.default.copy(alpha = 0.2f),
            strokeWidth = 1.dp
        ),
        yAxis = BarStyle.YAxis.Labels(
            ticks = 4,
            textStyle = typography.b10Reg().copy(color = colors.text.secondary),
            formatter = formatter,
            tickMarkColor = colors.border.default.copy(alpha = 0.4f),
            tickMarkWidth = 1.dp,
            tickMarkLength = 6.dp
        ),
        peek = BarStyle.Peek.Visible(
            hitSlop = 26.dp,

            guideColor = colors.charts.bar.guide,
            guideWidth = 1.dp,
            guideDash = 6.dp,
            guideGap = 6.dp,

            focusColor = colors.charts.bar.focus,
            focusRadius = 3.5.dp,
            focusRingWidth = 2.dp,
            focusHaloRadius = 18.dp,

            tooltipBackground = colors.charts.tooltip.background,
            tooltipBorder = colors.charts.tooltip.border,
            tooltipText = colors.charts.tooltip.text,
            tooltipCornerRadius = 10.dp,
            tooltipPaddingH = 10.dp,
            tooltipPaddingV = 6.dp,
            tooltipMargin = 10.dp,

            decimals = 0,
            showLabel = true,
        ),
        yAxisLine = BarStyle.AxisLine(
            color = colors.border.default.copy(alpha = 0.4f),
            width = 1.dp
        ),
        xAxis = BarStyle.XAxis.LabelsAdaptive(
            textStyle = typography.b10Reg()
                .copy(color = colors.text.tertiary),
            minGapDp = 8.dp
        ),
        xBaseline = BarStyle.Baseline(
            color = colors.border.default.copy(alpha = 0.45f),
            width = 1.dp
        ),
        bars = BarStyle.Bars(
            corner = 12.dp,
            brushProvider = { entry, _, rect ->
                val topColor = lerp(entry.color, colors.static.white, 0.25f)
                val midColor = entry.color
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to topColor,
                        0.6f to midColor,
                        1f to midColor.copy(alpha = 0.85f)
                    ),
                    startY = rect.top,
                    endY = rect.bottom
                )
            },
            strokeWidth = 0.dp,
            strokeColor = Color.Transparent,
            sizing = BarStyle.BarsSizing.AutoEqualBarsAndGaps(
                midThresholdDp = 22.dp,
                denseThresholdDp = 12.dp,
                midRatio = 0.55f,
                denseRatio = 0.35f,
                maxBarWidth = 56.dp
            )
        ),
        values = BarStyle.Values.Above(
            textStyle = typography.b10Bold().copy(color = colors.text.primary),
            formatter = formatter
        ),
    )

    BarChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

private fun Float.prettyFormat(): String {
    val absValue = abs(this)
    return when {
        absValue >= 1_000_000f -> formatCompact(this, 1_000_000, "M")
        absValue >= 1_000f -> formatCompact(this, 1_000, "K")
        absValue >= 1f -> roundToInt().toString()
        absValue == 0f -> "0"
        else -> formatCompact(this, 1, "", decimalsOverride = 1)
    }
}

private fun formatCompact(
    value: Float,
    scale: Int,
    suffix: String,
    decimalsOverride: Int? = null
): String {
    val sign = if (value < 0f) "-" else ""
    val absValue = abs(value)
    val scaled = absValue / scale.toFloat()
    val decimals = decimalsOverride ?: if (scaled < 10f) 1 else 0
    val factor = when (decimals) {
        0 -> 1
        1 -> 10
        2 -> 100
        else -> 1
    }
    val rounded = (scaled * factor).roundToInt()
    val integerPart = rounded / factor
    val fractionalPart = rounded % factor
    return buildString {
        append(sign)
        append(integerPart)
        if (decimals > 0 && fractionalPart != 0) {
            append('.')
            append(fractionalPart.toString().padStart(decimals, '0'))
        }
        append(suffix)
    }
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
