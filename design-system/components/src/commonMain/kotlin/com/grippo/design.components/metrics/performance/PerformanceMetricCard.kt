package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
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
import com.grippo.design.resources.provider.metric_overview_density
import com.grippo.design.resources.provider.metric_overview_duration
import com.grippo.design.resources.provider.metric_overview_intensity
import com.grippo.design.resources.provider.metric_overview_performance_average
import com.grippo.design.resources.provider.metric_overview_performance_comparison
import com.grippo.design.resources.provider.metric_overview_repetitions
import com.grippo.design.resources.provider.metric_overview_volume

private const val EMPTY_VALUE_PLACEHOLDER: String = "—"

@Composable
public fun PerformanceMetricCard(
    modifier: Modifier = Modifier,
    metric: PerformanceMetricState,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
) {
    val empty = metric.isEmpty()
    val hasComparison = metric.hasComparison()
    val fullMode = !empty && hasComparison
    val showStatus = fullMode && (
            presentation == MetricCardPresentation.Detailed ||
                    metric.type != PerformanceMetricTypeState.Duration
            )

    val (accentColor, accentBackground) = performanceStatusColors(
        status = if (fullMode && presentation == MetricCardPresentation.Detailed) {
            metric.status
        } else {
            PerformanceTrendStatusState.Empty
        },
    )
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
        if (presentation == MetricCardPresentation.Overview) {
            MetricOverviewHeader(title = performanceOverviewLabel(metric.type))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconShape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

                Box(
                    modifier = Modifier
                        .clip(iconShape)
                        .background(accentBackground, shape = iconShape)
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
                )
            }
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = if (empty) EMPTY_VALUE_PLACEHOLDER else metric.currentDisplay(),
            style = if (presentation == MetricCardPresentation.Overview) {
                AppTokens.typography.h2()
            } else {
                AppTokens.typography.h4()
            },
            color = valueColor,
        )

        if (showStatus) {
            PerformanceTrendChip(status = metric.status)
        }

        if (fullMode && presentation == MetricCardPresentation.Overview) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                if (metric is PerformanceMetricState.Duration) {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.metric_overview_performance_comparison,
                            formatTrendDelta(metric.currentVsAveragePercentage),
                        ),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                } else {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.metric_overview_performance_average,
                            metric.averageDisplay(),
                        ),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )

                    Text(
                        text = formatTrendDelta(metric.currentVsAveragePercentage),
                        style = AppTokens.typography.b13Semi(),
                        color = performanceAverageDeltaColor(metric),
                    )
                }
            }
        }

        if (fullMode && presentation == MetricCardPresentation.Detailed) {
            val vsAverage = AppTokens.strings.res(Res.string.highlight_vs_average)
            val averageDisplay = metric.averageDisplay()
            val bestLabel =
                AppTokens.strings.res(Res.string.highlight_best_value, metric.bestDisplay())

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                itemVerticalAlignment = Alignment.CenterVertically,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = "$vsAverage $averageDisplay",
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )

                Text(
                    text = formatTrendDelta(metric.currentVsAveragePercentage),
                    style = AppTokens.typography.b13Semi(),
                    color = performanceAverageDeltaColor(metric),
                )
            }

            Text(
                text = bestLabel,
                style = AppTokens.typography.b11Semi(),
                color = AppTokens.colors.text.tertiary
            )
        }
    }
}

@Composable
private fun performanceOverviewLabel(type: PerformanceMetricTypeState): String {
    val resource = when (type) {
        PerformanceMetricTypeState.Duration -> Res.string.metric_overview_duration
        PerformanceMetricTypeState.Volume -> Res.string.metric_overview_volume
        PerformanceMetricTypeState.Density -> Res.string.metric_overview_density
        PerformanceMetricTypeState.Intensity -> Res.string.metric_overview_intensity
        PerformanceMetricTypeState.Repetitions -> Res.string.metric_overview_repetitions
    }
    return AppTokens.strings.res(resource)
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

        PerformanceTrendStatusState.Empty -> return
    }

    val (_, background) = performanceStatusColors(status)

    val shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

    Text(
        modifier = Modifier
            .clip(shape)
            .background(background, shape = shape)
            .padding(
                horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                vertical = AppTokens.dp.metrics.status.verticalPadding
            ),
        text = label,
        style = AppTokens.typography.b11Semi(),
        color = AppTokens.colors.text.primary,
    )
}

@Composable
private fun performanceStatusColors(status: PerformanceTrendStatusState): Pair<Color, Color> {
    return when (status) {
        PerformanceTrendStatusState.Empty ->
            AppTokens.colors.text.tertiary to AppTokens.colors.background.screen

        PerformanceTrendStatusState.Record,
        PerformanceTrendStatusState.Improved ->
            AppTokens.colors.semantic.success to AppTokens.colors.semantic.successBackground

        PerformanceTrendStatusState.Stable ->
            AppTokens.colors.text.secondary to AppTokens.colors.background.screen

        PerformanceTrendStatusState.Declined ->
            AppTokens.colors.semantic.warning to AppTokens.colors.semantic.warningBackground
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
        PerformanceMetricGridPreview(presentation = MetricCardPresentation.Detailed)
    }
}

@Composable
private fun PerformanceMetricGridPreview(presentation: MetricCardPresentation) {
    val metrics = stubPerformanceMetrics()
    val gridMetrics = if (presentation == MetricCardPresentation.Overview) {
        metrics.filterIsInstance<PerformanceMetricState.Duration>().forEach { metric ->
            PerformanceMetricCard(
                modifier = Modifier.fillMaxWidth(),
                metric = metric,
                presentation = presentation,
            )
        }
        metrics.filterNot { it is PerformanceMetricState.Duration }
    } else {
        metrics
    }

    gridMetrics.chunked(2).forEach { rowMetrics ->
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            rowMetrics.forEach { metric ->
                PerformanceMetricCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    metric = metric,
                    presentation = presentation,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardOverviewPreview() {
    PreviewContainer {
        PerformanceMetricGridPreview(presentation = MetricCardPresentation.Overview)
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardOverviewEmptyPreview() {
    PreviewContainer {
        PerformanceMetricCard(
            metric = stubEmptyPerformanceMetric(),
            presentation = MetricCardPresentation.Overview,
        )
        val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
        PerformanceMetricCard(
            metric = metric.copy(
                average = VolumeFormatState.of(0f),
                currentVsAveragePercentage = 0,
                status = PerformanceTrendStatusState.Stable,
            ),
            presentation = MetricCardPresentation.Overview,
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
        val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
        PerformanceMetricCard(
            metric = metric.copy(
                average = VolumeFormatState.of(0f),
                currentVsAveragePercentage = 0,
                status = PerformanceTrendStatusState.Stable,
            ),
        )
    }
}
