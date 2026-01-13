package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.grippo.chart.sparkline.Sparkline
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.chart.sparkline.SparklineStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun PerformanceTrendChartSection(
    modifier: Modifier = Modifier,
    chartPoints: ImmutableList<Float>,
) {
    val data = remember(chartPoints) {
        if (chartPoints.size < 2) {
            null
        } else {
            val points = chartPoints.mapIndexed { index, value ->
                SparklinePoint(index.toFloat(), value)
            }
            SparklineData(points = points)
        }
    } ?: return

    Sparkline(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppTokens.dp.metrics.performanceTrend.height)
            .aspectRatio(2.2f),
        data = data,
        style = performanceSparklineStyle()
    )
}

@Composable
private fun performanceSparklineStyle(): SparklineStyle {
    val charts = AppTokens.colors.charts

    return SparklineStyle(
        line = SparklineStyle.Line(
            stroke = 2.dp,
            color = charts.sparkline.lineA,
            brush = null,
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
        dots = SparklineStyle.Dots.Visible(radius = 2.dp, color = null),
        extremes = SparklineStyle.Extremes.Visible(
            minColor = AppTokens.colors.semantic.warning,
            maxColor = AppTokens.colors.semantic.success,
            radius = 3.dp
        )
    )
}

@AppPreview
@Composable
private fun PerformanceTrendChartSectionPreview() {
    PreviewContainer {
        PerformanceTrendChartSection(
            chartPoints = persistentListOf(4f, 6f, 5f, 8f, 9f, 7f)
        )
    }
}
