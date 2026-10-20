package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.chart.sparkline.Sparkline
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.chart.sparkline.SparklineStyle
import com.grippo.chart.sparkline.SparklineStyle.Peek
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun Sparkline(
    modifier: Modifier = Modifier,
    data: SparklineData,
    valueFormatter: ((Float) -> String)? = null,
) {
    val charts = AppTokens.colors.charts

    val style = SparklineStyle(
        line = SparklineStyle.Line(
            stroke = AppTokens.dp.metrics.charts.sparkline.lineWidth,
            color = AppTokens.colors.charts.sparkline.lineA,
            brush = null,
            curved = true,
            curveSmoothness = 0.25f,
            clampOvershoot = true,
        ),
        fill = SparklineStyle.Fill(
            provider = { rect ->
                Brush.verticalGradient(
                    0f to charts.sparkline.fillStart,
                    1f to charts.sparkline.fillEnd,
                    startY = rect.top,
                    endY = rect.bottom
                )
            }
        ),
        baseline = SparklineStyle.Baseline.None,
        midline = SparklineStyle.Midline.None,
        peek = Peek.Visible(
            hitSlop = AppTokens.dp.metrics.charts.peek.hitSlop,

            guideColor = charts.tooltip.guide,
            guideWidth = AppTokens.dp.metrics.charts.peek.guideWidth,
            guideDash = AppTokens.dp.metrics.charts.peek.guideDash,
            guideGap = AppTokens.dp.metrics.charts.peek.guideGap,

            focusColor = charts.sparkline.lineA,
            focusRadius = AppTokens.dp.metrics.charts.peek.focusRadius,
            focusRingWidth = AppTokens.dp.metrics.charts.peek.focusRingWidth,
            focusHaloRadius = AppTokens.dp.metrics.charts.peek.focusHaloRadius,

            tooltipBackground = charts.tooltip.background,
            tooltipBorder = charts.tooltip.border,
            tooltipText = charts.tooltip.text,
            tooltipCornerRadius = AppTokens.dp.metrics.charts.peek.tooltipRadius,
            tooltipPaddingH = AppTokens.dp.metrics.charts.peek.tooltipHorizontalPadding,
            tooltipPaddingV = AppTokens.dp.metrics.charts.peek.tooltipVerticalPadding,
            tooltipMargin = AppTokens.dp.metrics.charts.peek.tooltipMargin,

            decimals = 0,
            showLabel = true,
            valueFormatter = valueFormatter,
            valueTextStyle = AppTokens.typography.b12Semi(),
            labelTextStyle = AppTokens.typography.b10Med(),
            tooltipBorderWidth = AppTokens.dp.metrics.charts.peek.tooltipBorderWidth,
            tooltipLabelSpacing = AppTokens.dp.metrics.charts.peek.tooltipLabelSpacing,
        ),
        dots = SparklineStyle.Dots.None,
        extremes = SparklineStyle.Extremes.None,
        verticalPadding = AppTokens.dp.metrics.charts.sparkline.verticalPadding,
    )

    val dateRange = remember(data.points) {
        val first = data.points.minByOrNull { it.x }
        val last = data.points.maxByOrNull { it.x }
        if (first != null && last != null && first.x < last.x && first.label != last.label &&
            !first.label.isNullOrBlank() && !last.label.isNullOrBlank()
        ) {
            first.label to last.label
        } else {
            null
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.charts.sparkline.axisLabelSpacing),
    ) {
        Sparkline(
            modifier = Modifier.fillMaxWidth().weight(1f),
            data = data,
            style = style,
        )
        if (dateRange != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.charts.sparkline.axisLabelSpacing),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = dateRange.first.orEmpty(),
                    style = AppTokens.typography.b10Med(),
                    color = AppTokens.colors.text.secondary,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = dateRange.second.orEmpty(),
                    style = AppTokens.typography.b10Med(),
                    color = AppTokens.colors.text.secondary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun SparklinePreview() {
    PreviewContainer {
        val ds = SparklineData(
            points = listOf(4f, 6f, 5f, 8f, 9f, 7f, 12f, 10f, 13f, 11f, 16f)
                .mapIndexed { i, v -> SparklinePoint(i.toFloat(), v, "${i + 1} Oct") }
        )

        Sparkline(
            modifier = Modifier.size(300.dp),
            data = ds
        )
    }
}

@AppPreview
@Composable
private fun SparklineEmptyPreview() {
    PreviewContainer {
        Sparkline(
            modifier = Modifier.size(300.dp),
            data = SparklineData(points = emptyList()),
        )
    }
}

@AppPreview
@Composable
private fun SparklineSinglePointPreview() {
    PreviewContainer {
        Sparkline(
            modifier = Modifier.size(300.dp),
            data = SparklineData(points = listOf(SparklinePoint(0f, 82f))),
        )
    }
}

@AppPreview
@Composable
private fun SparklineFlatPreview() {
    PreviewContainer {
        Sparkline(
            modifier = Modifier.size(300.dp),
            data = SparklineData(
                points = List(7) { day -> SparklinePoint(day.toFloat(), 82f, "${day + 1} Oct") },
            ),
        )
    }
}

@AppPreview
@Composable
private fun SparklineDensePreview() {
    PreviewContainer {
        Sparkline(
            modifier = Modifier.size(300.dp),
            data = SparklineData(
                points = List(60) { day ->
                    val date = if (day < 31) "${day + 1} Oct" else "${day - 30} Nov"
                    SparklinePoint(day.toFloat(), 80f + day % 5, date)
                },
            ),
        )
    }
}

@AppPreview
@Composable
private fun SparklineCompactDatesPreview() {
    PreviewContainer {
        Sparkline(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.metrics.performance.strength.height),
            data = SparklineData(
                points = listOf(
                    SparklinePoint(0f, 80f, "1 October 2026 18:30"),
                    SparklinePoint(3f, 83f, "4 October 2026 18:30"),
                    SparklinePoint(17f, 81f, "18 October 2026 18:30"),
                ),
            ),
            valueFormatter = { value -> "${value.toInt()} kg" },
        )
    }
}
