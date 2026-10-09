package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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

@Immutable
public enum class BarChartXAxisLabels {
    WithLabels,
    WithoutLabels,
}

@Composable
internal fun BarChart(
    modifier: Modifier = Modifier,
    data: BarData,
    xAxisLabels: BarChartXAxisLabels = BarChartXAxisLabels.WithLabels,
) {
    val colors = AppTokens.colors
    val typography = AppTokens.typography

    val formatter: (Float, BarData) -> String = { value, _ -> value.prettyFormat() }

    val style = BarStyle(
        layout = BarStyle.Layout(
            labelPadding = AppTokens.dp.metrics.charts.bar.labelPadding,
            chartPadding = BarStyle.ChartPadding(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = 0.dp
            ),
            minBarHeight = AppTokens.dp.metrics.charts.bar.minHeight,
            yAxisLabelSpacing = AppTokens.dp.metrics.charts.bar.axisLabelSpacing,
            valueLabelSpacing = AppTokens.dp.metrics.charts.bar.contentSpacing,
            baselineSpacing = AppTokens.dp.metrics.charts.bar.contentSpacing,
            barsAxisInsetStart = AppTokens.dp.metrics.charts.bar.contentSpacing,
            barsAxisInsetEnd = AppTokens.dp.metrics.charts.bar.contentSpacing
        ),
        grid = BarStyle.Grid(
            show = true,
            color = colors.divider.default.copy(alpha = 0.2f),
            strokeWidth = AppTokens.dp.metrics.charts.axis.strokeWidth
        ),
        yAxis = BarStyle.YAxis.Labels(
            ticks = 4,
            textStyle = typography.b10Reg().copy(color = colors.text.secondary),
            formatter = formatter,
            tickMarkColor = colors.border.default.copy(alpha = 0.4f),
            tickMarkWidth = AppTokens.dp.metrics.charts.axis.strokeWidth,
            tickMarkLength = AppTokens.dp.metrics.charts.axis.tickLength
        ),
        peek = BarStyle.Peek.Visible(
            hitSlop = AppTokens.dp.metrics.charts.peek.hitSlop,

            guideColor = colors.charts.tooltip.guide,
            guideWidth = AppTokens.dp.metrics.charts.peek.guideWidth,
            guideDash = AppTokens.dp.metrics.charts.peek.guideDash,
            guideGap = AppTokens.dp.metrics.charts.peek.guideGap,

            focusColor = colors.charts.tooltip.focus,
            focusRadius = AppTokens.dp.metrics.charts.peek.focusRadius,
            focusRingWidth = AppTokens.dp.metrics.charts.peek.focusRingWidth,
            focusHaloRadius = AppTokens.dp.metrics.charts.peek.focusHaloRadius,

            tooltipBackground = colors.charts.tooltip.background,
            tooltipBorder = colors.charts.tooltip.border,
            tooltipText = colors.charts.tooltip.text,
            tooltipCornerRadius = AppTokens.dp.metrics.charts.peek.tooltipRadius,
            tooltipPaddingH = AppTokens.dp.metrics.charts.peek.tooltipHorizontalPadding,
            tooltipPaddingV = AppTokens.dp.metrics.charts.peek.tooltipVerticalPadding,
            tooltipMargin = AppTokens.dp.metrics.charts.peek.tooltipMargin,

            decimals = 0,
            showLabel = true,
        ),
        yAxisLine = BarStyle.AxisLine(
            color = colors.border.default.copy(alpha = 0.4f),
            width = AppTokens.dp.metrics.charts.axis.strokeWidth
        ),
        xAxis = if (xAxisLabels == BarChartXAxisLabels.WithLabels) {
            BarStyle.XAxis.LabelsAdaptive(
                textStyle = typography.b10Reg()
                    .copy(color = colors.text.tertiary),
                minGapDp = AppTokens.dp.metrics.charts.bar.contentSpacing
            )
        } else {
            BarStyle.XAxis.None
        },
        xBaseline = BarStyle.Baseline(
            color = colors.border.default.copy(alpha = 0.45f),
            width = AppTokens.dp.metrics.charts.axis.strokeWidth
        ),
        bars = BarStyle.Bars(
            corner = AppTokens.dp.metrics.charts.bar.radius,
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
                midThresholdDp = AppTokens.dp.metrics.charts.bar.midDensityThreshold,
                denseThresholdDp = AppTokens.dp.metrics.charts.bar.denseThreshold,
                midRatio = 0.55f,
                denseRatio = 0.35f,
                maxBarWidth = AppTokens.dp.metrics.charts.bar.maxWidth
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
                BarEntry("Mon", 6f, AppTokens.colors.palette.palette7BlueGrowth[0]),
                BarEntry("Tue", 10f, AppTokens.colors.palette.palette7BlueGrowth[1]),
                BarEntry("Wed", 4f, AppTokens.colors.palette.palette7BlueGrowth[2]),
                BarEntry("Thu", 12f, AppTokens.colors.palette.palette7BlueGrowth[3]),
                BarEntry("Fri", 8f, AppTokens.colors.palette.palette7BlueGrowth[4]),
                BarEntry("Sat", 14f, AppTokens.colors.palette.palette7BlueGrowth[5]),
                BarEntry("Sun", 9f, AppTokens.colors.palette.palette7BlueGrowth[6]),
                BarEntry("Sun", 9f, AppTokens.colors.palette.palette7BlueGrowth[0]),
                BarEntry("Sun", 9f, AppTokens.colors.palette.palette7BlueGrowth[1]),
                BarEntry("Sun", 9f, AppTokens.colors.palette.palette7BlueGrowth[2]),
                BarEntry("Sun", 9f, AppTokens.colors.palette.palette7BlueGrowth[3]),
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
