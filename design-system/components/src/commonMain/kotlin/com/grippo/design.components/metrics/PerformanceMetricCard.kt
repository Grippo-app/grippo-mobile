package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceTrendStatusState
import com.grippo.core.state.metrics.stubPerformanceMetrics
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_best_value
import com.grippo.design.resources.provider.highlight_status_declined
import com.grippo.design.resources.provider.highlight_status_improved
import com.grippo.design.resources.provider.highlight_status_record
import com.grippo.design.resources.provider.highlight_status_stable
import com.grippo.design.resources.provider.highlight_vs_average

@Composable
public fun PerformanceMetricCard(
    metric: PerformanceMetricState,
    modifier: Modifier = Modifier,
) {
    val accentColor = performanceStatusColor(metric.status)

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        val label = metric.type.label()

        val averageDelta = formatTrendDelta(metric.currentVsAveragePercentage)
        val averageDeltaColor = performanceAverageDeltaColor(metric)

        val vsAverage = AppTokens.strings.res(Res.string.highlight_vs_average)

        val (current, average, bestLabel) = when (metric) {
            is PerformanceMetricState.Duration -> {
                val best = AppTokens.strings.res(
                    Res.string.highlight_best_value,
                    metric.best.display
                )
                Triple(
                    metric.current.display,
                    metric.average.display,
                    best
                )
            }

            is PerformanceMetricState.Volume -> {
                val best = AppTokens.strings.res(
                    Res.string.highlight_best_value,
                    metric.best.short()
                )
                Triple(metric.current.short(), metric.average.short(), best)
            }

            is PerformanceMetricState.Density -> {
                val best = AppTokens.strings.res(
                    Res.string.highlight_best_value,
                    metric.best.short()
                )
                Triple(metric.current.short(), metric.average.short(), best)
            }

            is PerformanceMetricState.Repetitions -> {
                val best = AppTokens.strings.res(
                    Res.string.highlight_best_value,
                    metric.best.short()
                )
                Triple(metric.current.short(), metric.average.short(), best)
            }

            is PerformanceMetricState.Intensity -> {
                val best = AppTokens.strings.res(
                    Res.string.highlight_best_value,
                    metric.best.short()
                )
                Triple(metric.current.short(), metric.average.short(), best)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconShape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

            Box(
                modifier = Modifier
                    .clip(iconShape)
                    .background(accentColor.copy(alpha = 0.14f), shape = iconShape)
                    .padding(AppTokens.dp.contentPadding.text),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(AppTokens.dp.metrics.performanceTrend.icon),
                    imageVector = metric.type.icon(),
                    tint = accentColor,
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            PerformanceTrendChip(status = metric.status)
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "$vsAverage $average",
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = averageDelta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTokens.typography.b13Semi(),
                color = averageDeltaColor
            )
        }

        Text(
            text = bestLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTokens.typography.b11Semi(),
            color = AppTokens.colors.text.tertiary
        )
    }
}

@Composable
private fun performanceAverageDeltaColor(metric: PerformanceMetricState): Color {
    if (metric is PerformanceMetricState.Duration) {
        return AppTokens.colors.text.secondary
    }

    return when {
        metric.currentVsAveragePercentage > 0 -> AppTokens.colors.semantic.success
        metric.currentVsAveragePercentage < 0 -> AppTokens.colors.semantic.warning
        else -> AppTokens.colors.text.secondary
    }
}

@Composable
private fun PerformanceTrendChip(status: PerformanceTrendStatusState) {
    val label = when (status) {
        PerformanceTrendStatusState.Record ->
            AppTokens.strings.res(Res.string.highlight_status_record)

        PerformanceTrendStatusState.Improved ->
            AppTokens.strings.res(Res.string.highlight_status_improved)

        PerformanceTrendStatusState.Stable ->
            AppTokens.strings.res(Res.string.highlight_status_stable)

        PerformanceTrendStatusState.Declined ->
            AppTokens.strings.res(Res.string.highlight_status_declined)
    }

    val color = performanceStatusColor(status)

    val shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

    Text(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.18f), shape = shape)
            .padding(
                horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                vertical = AppTokens.dp.metrics.status.verticalPadding
            ),
        text = label,
        style = AppTokens.typography.b11Semi(),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun performanceStatusColor(status: PerformanceTrendStatusState): Color {
    return when (status) {
        PerformanceTrendStatusState.Record,
        PerformanceTrendStatusState.Improved -> AppTokens.colors.semantic.success

        PerformanceTrendStatusState.Stable -> AppTokens.colors.text.secondary
        PerformanceTrendStatusState.Declined -> AppTokens.colors.semantic.warning
    }
}

private fun formatTrendDelta(delta: Int): String {
    return when {
        delta > 0 -> "+${delta}%"
        delta < 0 -> "${delta}%"
        else -> "0%"
    }
}

@AppPreview
@Composable
private fun PerformanceTrendCardPreviewCard() {
    PreviewContainer {
        PerformanceMetricCard(
            modifier = Modifier.size(150.dp),
            metric = stubPerformanceMetrics().random()
        )

        PerformanceMetricCard(
            metric = stubPerformanceMetrics().random()
        )
        PerformanceMetricCard(
            metric = stubPerformanceMetrics().random()
        )
        PerformanceMetricCard(
            metric = stubPerformanceMetrics().random()
        )
    }
}
