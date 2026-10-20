package com.grippo.performance.trend.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceTrendHistory
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.performance_trend_details_value_unavailable
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat

@Composable
internal fun PerformanceHistoryRow(
    modifier: Modifier = Modifier,
    entry: PerformanceTrendHistoryEntry,
    showTime: Boolean,
) {
    val timestamp = entry.endDate.value
    val isAvailable = entry.metric.chartValue() != null

    Row(
        modifier = modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = entry.endDate.short(),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
            if (showTime && timestamp != null) {
                Text(
                    text = DateCompose.rememberFormat(
                        value = timestamp,
                        format = DateFormat.TimeOnly.Time24hHm,
                    ),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
        Text(
            modifier = Modifier.weight(1f),
            text = if (isAvailable) {
                entry.metric.currentDisplay()
            } else {
                AppTokens.strings.res(Res.string.performance_trend_details_value_unavailable)
            },
            style = if (isAvailable) AppTokens.typography.b14Semi() else AppTokens.typography.b12Med(),
            color = if (isAvailable) AppTokens.colors.text.primary else AppTokens.colors.text.secondary,
            textAlign = TextAlign.End,
        )
    }
}

@AppPreview
@Composable
private fun PerformanceHistoryRowPreview() {
    PreviewContainer {
        val entry = stubPerformanceTrendHistory().first()
        PerformanceHistoryRow(
            modifier = Modifier.fillMaxWidth(),
            entry = entry,
            showTime = false,
        )
        PerformanceHistoryRow(
            modifier = Modifier.fillMaxWidth(),
            entry = entry,
            showTime = true,
        )
        PerformanceHistoryRow(
            modifier = Modifier.fillMaxWidth(),
            entry = entry.copy(metric = stubEmptyPerformanceMetric()),
            showTime = false,
        )
    }
}
