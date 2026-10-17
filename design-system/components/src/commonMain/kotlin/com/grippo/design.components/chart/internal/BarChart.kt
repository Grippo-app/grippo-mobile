package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.grippo.chart.bar.BarChart as ComposeBarChart
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.chart.bar.BarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
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
    valueFormatter: @Composable (Float) -> String,
    xAxisLabels: BarChartXAxisLabels = BarChartXAxisLabels.WithLabels,
    description: String? = null,
) {
    val colors = AppTokens.colors
    val typography = AppTokens.typography
    val dimensions = AppTokens.dp.metrics.charts
    val formattedValues = data.items.associate { entry -> entry.value to valueFormatter(entry.value) }
    val chartDescription = description ?: data.items.joinToString(separator = "; ") { entry ->
        "${entry.label}: ${formattedValues[entry.value].orEmpty()}"
    }

    val style = BarStyle(
        layout = BarStyle.Layout(
            labelPadding = dimensions.bar.axisLabelSpacing,
            xAxisLabelSpacing = dimensions.bar.axisLabelSpacing,
        ),
        grid = BarStyle.Grid(
            show = false,
            color = Color.Transparent,
            strokeWidth = 0.dp,
        ),
        yAxis = BarStyle.YAxis.None,
        yAxisLine = null,
        xAxis = if (xAxisLabels == BarChartXAxisLabels.WithLabels) {
            BarStyle.XAxis.LabelsAdaptive(
                textStyle = typography.b10Med().copy(color = colors.text.tertiary),
                minGapDp = dimensions.bar.axisLabelSpacing,
            )
        } else {
            BarStyle.XAxis.None
        },
        xBaseline = BarStyle.Baseline(
            color = colors.charts.bar.baseline,
            width = dimensions.axis.strokeWidth,
        ),
        bars = BarStyle.Bars(
            corner = dimensions.bar.radius,
            brushProvider = null,
            strokeWidth = 0.dp,
            strokeColor = Color.Transparent,
            sizing = BarStyle.BarsSizing.EvenSlots(
                maxBarWidth = dimensions.bar.maxWidth,
                widthFraction = dimensions.bar.widthFraction,
            ),
        ),
        values = BarStyle.Values.None,
        peek = BarStyle.Peek.Visible(
            hitSlop = dimensions.peek.hitSlop,
            guideColor = colors.charts.tooltip.guide,
            guideWidth = dimensions.peek.guideWidth,
            guideDash = dimensions.peek.guideDash,
            guideGap = dimensions.peek.guideGap,
            focusColor = colors.charts.tooltip.focus,
            focusRadius = 0.dp,
            focusRingWidth = 0.dp,
            focusHaloRadius = 0.dp,
            selectedBarStrokeColor = colors.charts.tooltip.focus,
            selectedBarStrokeWidth = dimensions.bar.selectionStrokeWidth,
            tooltipBackground = colors.charts.tooltip.background,
            tooltipBorder = colors.charts.tooltip.border,
            tooltipText = colors.charts.tooltip.text,
            tooltipCornerRadius = dimensions.peek.tooltipRadius,
            tooltipPaddingH = dimensions.peek.tooltipHorizontalPadding,
            tooltipPaddingV = dimensions.peek.tooltipVerticalPadding,
            tooltipMargin = dimensions.peek.tooltipMargin,
            valueTextStyle = typography.b12Semi(),
            labelTextStyle = typography.b10Med(),
            tooltipBorderWidth = dimensions.peek.tooltipBorderWidth,
            tooltipLabelSpacing = dimensions.peek.tooltipLabelSpacing,
            valueFormatter = { value, _ -> formattedValues[value].orEmpty() },
        ),
    )

    ComposeBarChart(
        modifier = modifier.semantics { contentDescription = chartDescription },
        data = data,
        style = style,
    )
}

@AppPreview
@Composable
private fun BarChartPreview() {
    PreviewContainer {
        BarChart(
            modifier = Modifier.size(300.dp),
            data = BarData(
                items = (0..10).map { index ->
                    BarEntry(
                        label = "Day ${index + 1}",
                        value = 4f + index % 5 * 2,
                        color = AppTokens.colors.palette.palette7BlueGrowth[2],
                    )
                },
            ),
            valueFormatter = { value -> value.roundToInt().toString() },
        )
    }
}

@AppPreview
@Composable
private fun BarChartWithoutLabelsPreview() {
    PreviewContainer {
        BarChart(
            modifier = Modifier.size(300.dp),
            data = BarData(
                items = listOf(
                    BarEntry(label = "First", value = 0f, color = AppTokens.colors.palette.palette7BlueGrowth.first()),
                    BarEntry(label = "Second", value = 1_250f, color = AppTokens.colors.palette.palette7BlueGrowth[2]),
                ),
            ),
            valueFormatter = { value -> value.roundToInt().toString() },
            xAxisLabels = BarChartXAxisLabels.WithoutLabels,
        )
    }
}

@AppPreview
@Composable
private fun BarChartDensePreview() {
    PreviewContainer {
        BarChart(
            modifier = Modifier.size(300.dp),
            data = BarData(
                items = (1..31).map { day ->
                    BarEntry(
                        label = day.toString(),
                        value = if (day % 5 == 0) 0f else 100f + day % 7 * 150,
                        color = AppTokens.colors.palette.palette7BlueGrowth[2],
                    )
                },
            ),
            valueFormatter = { value -> value.roundToInt().toString() },
        )
    }
}

@AppPreview
@Composable
private fun BarChartZeroPreview() {
    PreviewContainer {
        BarChart(
            modifier = Modifier.size(300.dp),
            data = BarData(
                items = (1..3).map { day ->
                    BarEntry(
                        label = "Day $day",
                        value = 0f,
                        color = AppTokens.colors.palette.palette7BlueGrowth[2],
                    )
                },
            ),
            valueFormatter = { value -> value.roundToInt().toString() },
        )
    }
}

@AppPreview
@Composable
private fun BarChartEmptyPreview() {
    PreviewContainer {
        BarChart(
            modifier = Modifier.size(300.dp),
            data = BarData(items = emptyList()),
            valueFormatter = { value -> value.roundToInt().toString() },
        )
    }
}
