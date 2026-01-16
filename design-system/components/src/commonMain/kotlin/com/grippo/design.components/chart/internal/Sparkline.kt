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
            stroke = 2.dp,
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
            color = charts.sparkline.middle,
            width = 1.dp,
            dash = 3.dp,
            gap = 3.dp
        ),
        peek = Peek.Visible(
            hitSlop = 26.dp,

            guideColor = charts.sparkline.middle.copy(alpha = 0.65f),
            guideWidth = 1.dp,
            guideDash = 6.dp,
            guideGap = 6.dp,

            focusColor = charts.sparkline.lineA,
            focusRadius = 3.5.dp,
            focusRingWidth = 2.dp,
            focusHaloRadius = 18.dp,

            tooltipBackground = charts.sparkline.tooltip.background,
            tooltipBorder = charts.sparkline.tooltip.border,
            tooltipText = charts.sparkline.tooltip.text,
            tooltipCornerRadius = 10.dp,
            tooltipPaddingH = 10.dp,
            tooltipPaddingV = 6.dp,
            tooltipMargin = 10.dp,

            decimals = 0,
            showLabel = true,
        ),
        dots = SparklineStyle.Dots.Visible(
            radius = 2.dp,
            color = null
        ),
        extremes = SparklineStyle.Extremes.Visible(
            minColor = AppTokens.colors.semantic.warning,
            maxColor = AppTokens.colors.semantic.success,
            radius = 3.dp
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