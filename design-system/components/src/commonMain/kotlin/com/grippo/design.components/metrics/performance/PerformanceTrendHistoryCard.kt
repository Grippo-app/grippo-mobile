package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
public fun PerformanceTrendHistoryCard(
    modifier: Modifier = Modifier,
    history: ImmutableList<PerformanceTrendHistoryEntry>,
) {
    val valueLabels = history.mapNotNull { entry ->
        val value = entry.metric.chartValue() ?: return@mapNotNull null
        value to entry.metric.currentDisplay()
    }.toMap()

    val dateLabels = history.asReversed().map { it.endDate.short() }

    val data = remember(history, dateLabels) {
        val points = history
            .asReversed()
            .mapIndexedNotNull { index, entry ->
                val value = entry.metric.chartValue() ?: return@mapIndexedNotNull null
                SparklinePoint(
                    x = index.toFloat(),
                    y = value,
                    label = dateLabels[index],
                )
            }

        if (points.size < 2) {
            null
        } else {
            SparklineData(points = points)
        }
    } ?: return

    Sparkline(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(AppTokens.dp.metrics.performance.trend.historyAspectRatio),
        data = data,
        valueFormatter = valueLabels::getValue,
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

@AppPreview
@Composable
private fun PerformanceTrendHistoryCardMissingValuePreview() {
    PreviewContainer {
        val history = performanceTrendPreviewHistory()
        PerformanceTrendHistoryCard(
            history = persistentListOf(
                history[0],
                history[1].copy(metric = stubEmptyPerformanceMetric()),
                history[2],
            ),
        )
    }
}

private fun performanceTrendPreviewHistory(): ImmutableList<PerformanceTrendHistoryEntry> {
    val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
    return persistentListOf(1_200f, 900f, 780f).mapIndexed { index, value ->
        PerformanceTrendHistoryEntry(
            trainingId = "preview-performance-history-$index",
            endDate = DateTimeFormatState.of(
                value = LocalDateTime(2026, 10, 15 - index, 10, 0),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            metric = metric.copy(current = value),
        )
    }.toPersistentList()
}
