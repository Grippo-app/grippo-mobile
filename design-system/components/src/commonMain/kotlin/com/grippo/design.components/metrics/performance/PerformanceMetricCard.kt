package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.performance.internal.PerformanceTrendChip
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.metric_overview_performance_average
import com.grippo.design.resources.provider.metric_overview_performance_comparison
import com.grippo.design.resources.provider.performance_comparison_unavailable
import com.grippo.design.resources.provider.performance_trend_details_comparison
import com.grippo.design.resources.provider.performance_trend_details_value_unavailable
import com.grippo.design.resources.provider.performance_trend_details_value_unavailable_description

@Immutable
public sealed interface PerformanceMetricCardStyle {
    @Immutable
    public data object Overview : PerformanceMetricCardStyle

    @Immutable
    public data class Detailed(
        public val reference: PerformanceReferenceState,
    ) : PerformanceMetricCardStyle
}

@Composable
public fun PerformanceMetricCard(
    modifier: Modifier = Modifier,
    metric: PerformanceMetricState,
    style: PerformanceMetricCardStyle,
) {
    when (style) {
        PerformanceMetricCardStyle.Overview -> OverviewPerformanceMetric(modifier = modifier, metric = metric)
        is PerformanceMetricCardStyle.Detailed -> DetailedPerformanceMetric(
            modifier = modifier,
            metric = metric,
            reference = style.reference,
        )
    }
}

@Composable
private fun OverviewPerformanceMetric(
    modifier: Modifier = Modifier,
    metric: PerformanceMetricState,
) {
    val empty = metric.isEmpty()
    val comparisonPercentage = metric.currentVsAveragePercentage

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        MetricOverviewHeader(title = metric.type.label())

        if (empty) {
            Text(
                text = AppTokens.strings.res(Res.string.performance_trend_details_value_unavailable),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = metric.currentDisplay(),
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
            )
        }

        if (metric.showsComparison()) {
            PerformanceTrendChip(status = metric.status, reference = PerformanceReferenceState.Average)
        }

        if (!empty && comparisonPercentage == null) {
            Text(
                text = AppTokens.strings.res(Res.string.performance_comparison_unavailable),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }

        if (!empty && comparisonPercentage != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                if (metric is PerformanceMetricState.Duration) {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.metric_overview_performance_comparison,
                            PercentageFormatState.shortDelta(comparisonPercentage),
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
                        text = PercentageFormatState.shortDelta(comparisonPercentage),
                        style = AppTokens.typography.b13Semi(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailedPerformanceMetric(
    modifier: Modifier = Modifier,
    metric: PerformanceMetricState,
    reference: PerformanceReferenceState,
) {
    val isAvailable = metric.chartValue() != null
    val comparison = metric.currentVsAveragePercentage?.takeIf { it.isFinite() }

    MetricSectionPanel(modifier = modifier, style = MetricSectionPanelStyle.Small) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.performance.trend.icon),
                imageVector = metric.type.icon(),
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = if (isAvailable) {
                    metric.currentDisplay()
                } else {
                    AppTokens.strings.res(Res.string.performance_trend_details_value_unavailable)
                },
                style = if (isAvailable) AppTokens.typography.h4() else AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }

        if (!isAvailable) {
            Text(
                text = AppTokens.strings.res(Res.string.performance_trend_details_value_unavailable_description),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        } else if (metric.hasAverage()) {
            Text(
                text = reference.detailedValue(metric.averageDisplay()),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
            Text(
                text = if (comparison != null) {
                    AppTokens.strings.res(
                        Res.string.performance_trend_details_comparison,
                        PercentageFormatState.shortDelta(comparison),
                    )
                } else {
                    reference.zeroValueNotice()
                },
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        } else {
            Text(
                text = reference.missingValueNotice(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun PerformanceTrendCardPreviewCard() {
    PreviewContainer {
        PerformanceMetricGridPreview(style = PerformanceMetricCardStyle.Detailed(PerformanceReferenceState.Average))
    }
}

@Composable
private fun PerformanceMetricGridPreview(style: PerformanceMetricCardStyle) {
    val metrics = stubPerformanceMetrics()
    val gridMetrics = if (style == PerformanceMetricCardStyle.Overview) {
        metrics.filterIsInstance<PerformanceMetricState.Duration>().forEach { metric ->
            PerformanceMetricCard(
                modifier = Modifier.fillMaxWidth(),
                metric = metric,
                style = style,
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
                    style = style,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardOverviewPreview() {
    PreviewContainer {
        PerformanceMetricGridPreview(style = PerformanceMetricCardStyle.Overview)
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardOverviewEmptyPreview() {
    PreviewContainer {
        PerformanceMetricCard(
            metric = stubEmptyPerformanceMetric(),
            style = PerformanceMetricCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardEmptyPreview() {
    PreviewContainer {
        PerformanceMetricCard(
            metric = stubEmptyPerformanceMetric(),
            style = PerformanceMetricCardStyle.Detailed(PerformanceReferenceState.Average),
        )
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardWithoutComparisonPreview() {
    PreviewContainer {
        val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
            .copy(
                average = null,
                currentVsAveragePercentage = null,
                status = PerformanceTrendStatusState.Stable,
            )
        PerformanceMetricCard(
            metric = metric,
            style = PerformanceMetricCardStyle.Overview,
        )
        PerformanceMetricCard(
            metric = metric,
            style = PerformanceMetricCardStyle.Detailed(PerformanceReferenceState.Average),
        )
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardRecordAfterZeroReferencePreview() {
    PreviewContainer {
        val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
            .copy(
                average = 0f,
                currentVsAveragePercentage = null,
                status = PerformanceTrendStatusState.Record,
            )
        PerformanceMetricCard(
            metric = metric,
            style = PerformanceMetricCardStyle.Overview,
        )
        PerformanceMetricCard(
            metric = metric,
            style = PerformanceMetricCardStyle.Detailed(PerformanceReferenceState.Average),
        )
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardDurationComparisonPreview() {
    PreviewContainer {
        val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Duration>().first()
        PerformanceMetricCard(
            metric = metric,
            style = PerformanceMetricCardStyle.Overview,
        )
        PerformanceMetricCard(
            metric = metric,
            style = PerformanceMetricCardStyle.Detailed(PerformanceReferenceState.Average),
        )
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardPreviousReferencePreview() {
    PreviewContainer {
        PerformanceMetricCard(
            metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first(),
            style = PerformanceMetricCardStyle.Detailed(PerformanceReferenceState.Previous),
        )
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardLargeAndFractionalPreview() {
    PreviewContainer {
        val volume = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
        val repetitions = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Repetitions>().first()
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            PerformanceMetricCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                metric = volume.copy(
                    current = 123_456.7f,
                    average = 100_000f,
                    best = 123_456.7f,
                    currentVsAveragePercentage = 23.4567,
                ),
                style = PerformanceMetricCardStyle.Overview,
            )
            PerformanceMetricCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Density>().first(),
                style = PerformanceMetricCardStyle.Overview,
            )
        }
        PerformanceMetricCard(
            metric = repetitions.copy(
                current = 84,
                average = 75.5,
                best = 92,
                currentVsAveragePercentage = 11.2583,
            ),
            style = PerformanceMetricCardStyle.Detailed(PerformanceReferenceState.Average),
        )
    }
}
