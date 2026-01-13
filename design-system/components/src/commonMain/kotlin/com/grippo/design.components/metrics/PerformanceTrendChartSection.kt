package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.grippo.chart.sparkline.Sparkline
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.chart.sparkline.SparklineStyle
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceTrendHistoryEntry
import com.grippo.core.state.metrics.stubPerformanceTrendHistory
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun PerformanceTrendChartSection(
    modifier: Modifier = Modifier,
    history: ImmutableList<PerformanceTrendHistoryEntry>,
) {
    val data = remember(history) {
        val values = history
            .asReversed()
            .mapNotNull { it.metric.chartValue() }

        if (values.size < 2) {
            null
        } else {
            val points = values.mapIndexed { index, value ->
                SparklinePoint(index.toFloat(), value)
            }
            SparklineData(points = points)
        }
    } ?: return

    Sparkline(
        modifier = modifier
            .fillMaxWidth()
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

private fun PerformanceMetricState.chartValue(): Float? {
    return when (this) {
        is PerformanceMetricState.Duration -> {
            val minutes = current.inWholeSeconds.toFloat() / 60f
            minutes.takeIf { it > 0f }
        }

        is PerformanceMetricState.Volume -> {
            current.value?.takeIf { it > 0f }
        }

        is PerformanceMetricState.Density -> {
            current.value?.takeIf { it > 0f }
        }

        is PerformanceMetricState.Repetitions -> {
            current.value?.toFloat()?.takeIf { it > 0f }
        }

        is PerformanceMetricState.Intensity -> {
            current.value?.takeIf { it > 0f }
        }
    }
}

@AppPreview
@Composable
private fun PerformanceTrendChartSectionPreview() {
    PreviewContainer {
        PerformanceTrendChartSection(
            history = stubPerformanceTrendHistory()
        )
    }
}
