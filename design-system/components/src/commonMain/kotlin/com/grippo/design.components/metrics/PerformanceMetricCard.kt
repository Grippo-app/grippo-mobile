package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.PerformanceTrendStatusState
import com.grippo.core.state.metrics.stubPerformanceMetrics
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.density
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.highlight_best_value
import com.grippo.design.resources.provider.highlight_status_declined
import com.grippo.design.resources.provider.highlight_status_improved
import com.grippo.design.resources.provider.highlight_status_record
import com.grippo.design.resources.provider.highlight_status_stable
import com.grippo.design.resources.provider.highlight_vs_average
import com.grippo.design.resources.provider.intensity_chip
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun PerformanceMetricCard(
    metric: PerformanceMetricState,
    modifier: Modifier = Modifier,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        val label = when (metric.type) {
            PerformanceMetricTypeState.Duration -> AppTokens.strings.res(Res.string.duration)
            PerformanceMetricTypeState.Volume -> AppTokens.strings.res(Res.string.volume)
            PerformanceMetricTypeState.Density -> AppTokens.strings.res(Res.string.density)
            PerformanceMetricTypeState.Repetitions -> AppTokens.strings.res(Res.string.repetitions)
            PerformanceMetricTypeState.Intensity -> AppTokens.strings.res(Res.string.intensity_chip)
        }

        val delta = formatTrendDelta(metric.deltaPercentage)

        val vsAverage = AppTokens.strings.res(Res.string.highlight_vs_average)

        val (current, average, bestLabel) = when (metric) {
            is PerformanceMetricState.Duration -> {
                val best = AppTokens.strings.res(
                    Res.string.highlight_best_value,
                    DateTimeUtils.format(metric.best)
                )
                Triple(
                    DateTimeUtils.format(metric.current),
                    DateTimeUtils.format(metric.average),
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
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.performanceTrend.icon),
                imageVector = metric.type.icon(),
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null
            )

            Text(
                text = label,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = delta,
                style = AppTokens.typography.h5(),
                color = performanceStatusColor(metric.status)
            )

            PerformanceTrendChip(status = metric.status)
        }

        Text(
            text = "$current · $vsAverage $average",
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = bestLabel,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary
        )
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
            .background(color.copy(alpha = 0.2f), shape = shape)
            .padding(
                horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                vertical = AppTokens.dp.metrics.status.verticalPadding
            ),
        text = label,
        style = AppTokens.typography.b11Semi(),
        color = color
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
