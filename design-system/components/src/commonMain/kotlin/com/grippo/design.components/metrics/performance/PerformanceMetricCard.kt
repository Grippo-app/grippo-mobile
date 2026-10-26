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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.performance.internal.PerformanceTrendChip
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.performance_comparison_unavailable
import com.grippo.design.resources.provider.performance_trend_details_comparison
import com.grippo.design.resources.provider.performance_trend_details_value_unavailable
import com.grippo.design.resources.provider.performance_trend_details_value_unavailable_description

@Composable
public fun PerformanceMetricCard(
    modifier: Modifier = Modifier,
    metric: PerformanceMetricState,
    reference: PerformanceReferenceState = PerformanceReferenceState.Average,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
) {
    if (presentation == MetricCardPresentation.Detailed) {
        DetailedPerformanceMetric(modifier = modifier, metric = metric, reference = reference)
        return
    }

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
            PerformanceTrendChip(status = metric.status, reference = reference)
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
                        text = reference.overviewComparison(PercentageFormatState.shortDelta(comparisonPercentage)),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                } else {
                    Text(
                        text = reference.overviewValue(metric.averageDisplay()),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )

                    Text(
                        text = PercentageFormatState.shortDelta(comparisonPercentage),
                        style = AppTokens.typography.b13Semi(),
                        color = metric.averageDeltaColor(),
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
            presentation = MetricCardPresentation.Overview,
        )
        PerformanceMetricCard(metric = metric)
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardRecordAfterZeroReferencePreview() {
    PreviewContainer {
        val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
        PerformanceMetricCard(
            metric = metric.copy(
                average = 0f,
                currentVsAveragePercentage = null,
                status = PerformanceTrendStatusState.Record,
            ),
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
            presentation = MetricCardPresentation.Overview,
        )
        PerformanceMetricCard(metric = metric)
    }
}

@AppPreview
@Composable
private fun PerformanceMetricCardPreviousReferencePreview() {
    PreviewContainer {
        val metrics = stubPerformanceMetrics()
        PerformanceMetricCard(
            metric = metrics.filterIsInstance<PerformanceMetricState.Volume>().first(),
            reference = PerformanceReferenceState.Previous,
            presentation = MetricCardPresentation.Overview,
        )
        PerformanceMetricCard(
            metric = metrics.filterIsInstance<PerformanceMetricState.Volume>().first(),
            reference = PerformanceReferenceState.Previous,
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
                presentation = MetricCardPresentation.Overview,
            )
            PerformanceMetricCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Density>().first(),
                presentation = MetricCardPresentation.Overview,
            )
        }
        PerformanceMetricCard(
            metric = repetitions.copy(
                current = 84,
                average = 75.5,
                best = 92,
                currentVsAveragePercentage = 11.2583,
            ),
        )
    }
}
