package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Composable
public fun PerformanceTrendHistoryCard(
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
            .aspectRatio(AppTokens.dp.metrics.performance.trend.historyAspectRatio),
        data = data,
    )
}

@AppPreview
@Composable
private fun PerformanceTrendHistoryCardPreview() {
    PreviewContainer {
        PerformanceTrendHistoryCard(
            history = performanceTrendPreviewHistory()
        )
    }
}

@AppPreview
@Composable
private fun PerformanceTrendHistoryCardEmptyPreview() {
    PreviewContainer {
        PerformanceTrendHistoryCard(history = persistentListOf())
        PerformanceTrendHistoryCard(history = persistentListOf(performanceTrendPreviewHistory().first()))
    }
}

private fun performanceTrendPreviewHistory(): ImmutableList<PerformanceTrendHistoryEntry> {
    val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
    val date = DateTimeFormatState.of(
        value = LocalDateTime(2026, 10, 15, 10, 0),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    return persistentListOf(
        PerformanceTrendHistoryEntry(endDate = date, metric = metric),
        PerformanceTrendHistoryEntry(endDate = date, metric = metric.copy(current = VolumeFormatState.of(900f))),
        PerformanceTrendHistoryEntry(endDate = date, metric = metric.copy(current = VolumeFormatState.of(780f))),
    )
}
