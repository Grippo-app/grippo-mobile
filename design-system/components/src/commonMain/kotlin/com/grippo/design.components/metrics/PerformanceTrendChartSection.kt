package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceTrendHistoryEntry
import com.grippo.core.state.metrics.stubPerformanceTrendHistory
import com.grippo.design.components.chart.internal.Sparkline
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
