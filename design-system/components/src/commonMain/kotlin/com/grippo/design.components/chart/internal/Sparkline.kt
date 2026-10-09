package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    data: SparklineData
) {
    val charts = AppTokens.colors.charts

    val style = SparklineStyle(
        line = SparklineStyle.Line(
            stroke = AppTokens.dp.metrics.charts.sparkline.lineWidth,
            color = AppTokens.colors.charts.sparkline.lineA,
            brush = { rect ->
                Brush.horizontalGradient(
                    listOf(charts.sparkline.lineA, charts.sparkline.lineB),
                    startX = rect.left,
                    endX = rect.right
                )
            },
            curved = true,
            curveSmoothness = 0.25f,
            clampOvershoot = true,
        ),
        fill = SparklineStyle.Fill(
            provider = { rect ->
                Brush.verticalGradient(
                    0f to charts.sparkline.fillBase.copy(alpha = 0.18f),
                    1f to charts.sparkline.fillBase.copy(alpha = 0f),
                    startY = rect.top,
                    endY = rect.bottom
                )
            }
        ),
        baseline = SparklineStyle.Baseline.None,
        midline = SparklineStyle.Midline.Visible(
            color = charts.tooltip.focus,
            width = AppTokens.dp.metrics.charts.axis.strokeWidth,
            dash = AppTokens.dp.metrics.charts.peek.guideDash,
            gap = AppTokens.dp.metrics.charts.peek.guideGap
        ),
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
        ),
        dots = SparklineStyle.Dots.Visible(
            radius = AppTokens.dp.metrics.charts.sparkline.dotRadius,
            color = null
        ),
        extremes = SparklineStyle.Extremes.Visible(
            minColor = AppTokens.colors.semantic.warning,
            maxColor = AppTokens.colors.semantic.success,
            radius = AppTokens.dp.metrics.charts.sparkline.markerRadius
        )
    )

    Sparkline(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun SparklinePreview() {
    PreviewContainer {
        val ds = SparklineData(
            points = listOf(4f, 6f, 5f, 8f, 9f, 7f, 12f, 10f, 13f, 11f, 16f)
                .mapIndexed { i, v -> SparklinePoint(i.toFloat(), v) }
        )

        Sparkline(
            modifier = Modifier.size(300.dp),
            data = ds
        )
    }
}
