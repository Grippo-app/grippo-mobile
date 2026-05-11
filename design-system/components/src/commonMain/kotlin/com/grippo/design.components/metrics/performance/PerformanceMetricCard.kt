package com.grippo.design.components.metrics.performance

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
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
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
    val empty = metric.isEmpty()
    val hasComparison = metric.hasComparison()
    val fullMode = !empty && hasComparison

    val accentColor = if (fullMode) {
        performanceStatusColor(metric.status)
    } else {
        AppTokens.colors.text.tertiary
    }
    val valueColor = if (empty) {
        AppTokens.colors.text.tertiary
    } else {
        AppTokens.colors.text.primary
    }
    val labelColor = if (empty) {
        AppTokens.colors.text.tertiary
    } else {
        AppTokens.colors.text.secondary
    }

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
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
                    modifier = Modifier.size(AppTokens.dp.metrics.performance.trend.icon),
                    imageVector = metric.type.icon(),
                    tint = accentColor,
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = metric.type.label(),
                style = AppTokens.typography.b12Semi(),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (fullMode) {
                PerformanceTrendChip(status = metric.status)
            }
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = if (empty) EMPTY_VALUE_PLACEHOLDER else metric.currentDisplay(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTokens.typography.h4(),
            color = valueColor,
        )

        if (fullMode) {
            val vsAverage = AppTokens.strings.res(Res.string.highlight_vs_average)
            val averageDisplay = metric.averageDisplay()
            val bestLabel =
                AppTokens.strings.res(Res.string.highlight_best_value, metric.bestDisplay())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "$vsAverage $averageDisplay",
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = formatTrendDelta(metric.currentVsAveragePercentage),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTokens.typography.b13Semi(),
                    color = performanceAverageDeltaColor(metric),
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
}

private const val EMPTY_VALUE_PLACEHOLDER: String = "—"

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
        PerformanceTrendStatusState.Empty -> return
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
        PerformanceTrendStatusState.Empty -> AppTokens.colors.text.tertiary

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

@AppPreview
@Composable
private fun PerformanceMetricCardEmptyPreview() {
    PreviewContainer {
        PerformanceMetricCard(
            metric = stubEmptyPerformanceMetric(),
        )
    }
}
