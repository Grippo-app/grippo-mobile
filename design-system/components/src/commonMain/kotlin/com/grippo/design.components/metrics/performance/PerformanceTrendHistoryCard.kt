package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceTrendHistory
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.daysUntil

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
        val chronological = history.asReversed()
        val origin = chronological.firstOrNull()?.endDate?.value
        val hasDates = chronological.all { it.endDate.value != null }
        val points = chronological
            .mapIndexedNotNull { index, entry ->
                val value = entry.metric.chartValue() ?: return@mapIndexedNotNull null
                val date = entry.endDate.value
                val elapsedDays = if (hasDates && origin != null && date != null) {
                    val days = origin.date.daysUntil(date.date)
                    val seconds = date.time.toSecondOfDay() - origin.time.toSecondOfDay()
                    (days.toDouble() + seconds / 86_400.0).toFloat()
                } else {
                    index.toFloat()
                }
                SparklinePoint(
                    x = elapsedDays,
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

    val description = data.points.joinToString("; ") { point ->
        "${point.label}: ${valueLabels.getValue(point.y)}"
    }

    Sparkline(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(AppTokens.dp.metrics.performance.trend.historyAspectRatio)
            .semantics { contentDescription = description },
        data = data,
        valueFormatter = valueLabels::getValue,
    )
}

@AppPreview
@Composable
private fun PerformanceTrendHistoryCardPreview() {
    PreviewContainer {
        PerformanceTrendHistoryCard(
            history = stubPerformanceTrendHistory()
        )
    }
}

@AppPreview
@Composable
private fun PerformanceTrendHistoryCardEmptyPreview() {
    PreviewContainer {
        PerformanceTrendHistoryCard(history = persistentListOf())
        PerformanceTrendHistoryCard(history = persistentListOf(stubPerformanceTrendHistory().first()))
    }
}

@AppPreview
@Composable
private fun PerformanceTrendHistoryCardMissingValuePreview() {
    PreviewContainer {
        val history = stubPerformanceTrendHistory()
        PerformanceTrendHistoryCard(
            history = persistentListOf(
                history[0],
                history[1].copy(metric = stubEmptyPerformanceMetric()),
                history[2],
            ),
        )
    }
}
