package com.grippo.performance.trend.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetric
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.core.state.metrics.performance.stubPerformanceTrendHistory
import com.grippo.design.components.datetime.DialogRangeHeader
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.metrics.performance.PerformanceMetricCard
import com.grippo.design.components.metrics.performance.PerformanceTrendHistoryCard
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.performance_history_empty
import com.grippo.design.resources.provider.performance_trend_details_empty_message
import com.grippo.design.resources.provider.performance_trend_details_latest
import com.grippo.design.resources.provider.performance_trend_details_recent_workouts
import com.grippo.performance.trend.details.components.PerformanceHistoryRow
import com.grippo.performance.trend.details.components.PerformanceMetricExplanation
import com.grippo.performance.trend.details.components.PerformanceTrendDetailsSkeleton
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
internal fun PerformanceTrendDetailsScreen(
    state: PerformanceTrendDetailsDialogState,
    loaders: ImmutableSet<PerformanceTrendDetailsLoader>,
    contract: PerformanceTrendDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    var isExpanded by rememberSaveable(state.range.value, state.metricType) {
        mutableStateOf(false)
    }

    DialogRangeHeader(text = state.range.shortStartEnd())

    PerformanceTrendDetailsContent(
        modifier = Modifier.fillMaxWidth().weight(1f, false),
        state = state,
        isExpanded = isExpanded,
        onToggleExplanation = { isExpanded = !isExpanded },
    )
}

@Composable
private fun PerformanceTrendDetailsContent(
    modifier: Modifier = Modifier,
    state: PerformanceTrendDetailsDialogState,
    isExpanded: Boolean,
    onToggleExplanation: () -> Unit,
) {
    val history = state.history
    val hasChart = history != null && history.count { it.metric.chartValue() != null } >= 2
    val listState = rememberAnchoredLazyListState(behavior = AnchorScrollBehavior.Animated)

    OverlayContainer(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        overlay = AppTokens.colors.background.dialog,
        content = { resolvedPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = resolvedPadding,
            ) {
                if (history == null) {
                    item(key = "skeleton", contentType = "skeleton") {
                        PerformanceTrendDetailsSkeleton(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    if (history.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = AppTokens.strings.res(Res.string.performance_history_empty),
                                    style = AppTokens.typography.h4(),
                                    color = AppTokens.colors.text.primary,
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = AppTokens.strings.res(Res.string.performance_trend_details_empty_message),
                                    style = AppTokens.typography.b14Med(),
                                    color = AppTokens.colors.text.secondary,
                                )
                            }
                        }
                    } else {
                        val latest = history.first()
                        val previousHistory = history.drop(1)

                        item(key = "latest", contentType = "summary") {
                            val timestamp = latest.endDate.value
                            val showTime = timestamp != null && history.any { entry ->
                                entry.trainingId != latest.trainingId && entry.endDate.value?.date == timestamp.date
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = AppTokens.strings.res(Res.string.performance_trend_details_latest),
                                        style = AppTokens.typography.b14Semi(),
                                        color = AppTokens.colors.text.primary,
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                                        horizontalAlignment = Alignment.End,
                                    ) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = latest.endDate.short(),
                                            style = AppTokens.typography.b12Med(),
                                            color = AppTokens.colors.text.secondary,
                                            textAlign = TextAlign.End,
                                        )
                                        if (showTime) {
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
                                }
                                PerformanceMetricCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    metric = latest.metric,
                                    reference = state.reference,
                                )
                            }
                        }

                        if (previousHistory.isNotEmpty()) {
                            item(key = "history_header", contentType = "heading") {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = AppTokens.dp.contentPadding.block),
                                    text = AppTokens.strings.res(Res.string.performance_trend_details_recent_workouts),
                                    style = AppTokens.typography.h4(),
                                    color = AppTokens.colors.text.primary,
                                )
                            }

                            if (hasChart) {
                                item(key = "chart", contentType = "chart") {
                                    PerformanceTrendHistoryCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = AppTokens.dp.contentPadding.content),
                                        history = history,
                                    )
                                }
                            }

                            itemsIndexed(
                                items = previousHistory,
                                key = { _, entry -> entry.trainingId },
                                contentType = { _, _ -> "workout" },
                            ) { index, entry ->
                                val date = entry.endDate.value?.date
                                val showTime = date != null && history.any { other ->
                                    other.trainingId != entry.trainingId && other.endDate.value?.date == date
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    PerformanceHistoryRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        entry = entry,
                                        showTime = showTime,
                                    )
                                    if (index < previousHistory.lastIndex) {
                                        ContentDivider()
                                    }
                                }
                            }
                        }
                    }

                    item(key = "explanation", contentType = "disclosure") {
                        PerformanceMetricExplanation(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppTokens.dp.contentPadding.block),
                            metricType = state.metricType,
                            reference = state.reference,
                            showChartScope = hasChart,
                            isExpanded = isExpanded,
                            onToggle = onToggleExplanation,
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

private fun performanceDetailsPreviewState(
    history: ImmutableList<PerformanceTrendHistoryEntry>? = stubPerformanceTrendHistory(),
    metricType: PerformanceMetricTypeState = PerformanceMetricTypeState.Volume,
    reference: PerformanceReferenceState = PerformanceReferenceState.Average,
): PerformanceTrendDetailsDialogState = PerformanceTrendDetailsDialogState(
    range = DateRangeFormatState.of(
        from = LocalDateTime(2026, 10, 11, 0, 0),
        to = LocalDateTime(2026, 10, 17, 23, 59),
    ),
    metricType = metricType,
    reference = reference,
    history = history,
)

@AppPreview
@Composable
private fun PopulatedPreview() {
    PreviewContainer {
        val entries = stubPerformanceTrendHistory()
        val history = (0 until 10).map { index ->
            entries[index % entries.size].copy(
                trainingId = "preview-performance-$index",
                endDate = DateTimeFormatState.of(
                    value = LocalDateTime(2026, 10, 17 - index / 2, if (index % 2 == 0) 18 else 9, 0),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateDdMmmYyyy,
                ),
            )
        }.toPersistentList()
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(history = history),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun PreviousReferencePreview() {
    PreviewContainer {
        val state = performanceDetailsPreviewState(reference = PerformanceReferenceState.Previous)
        BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
            DialogRangeHeader(text = state.range.shortStartEnd())
            PerformanceTrendDetailsContent(
                modifier = Modifier.fillMaxWidth().weight(1f, false),
                state = state,
                isExpanded = true,
                onToggleExplanation = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun EmptyPreview() {
    PreviewContainer {
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(history = persistentListOf()),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun LoadingPreview() {
    PreviewContainer {
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(history = null),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun SingleWorkoutPreview() {
    PreviewContainer {
        val entry = stubPerformanceTrendHistory().first()
        val metric = entry.metric as PerformanceMetricState.Volume
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(
                history = persistentListOf(
                    entry.copy(
                        metric = metric.copy(
                            average = null,
                            currentVsAveragePercentage = null,
                            status = PerformanceTrendStatusState.Stable,
                        ),
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TwoSameDayWorkoutsPreview() {
    PreviewContainer {
        val history = stubPerformanceTrendHistory()
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(
                history = persistentListOf(
                    history.first(),
                    history[1].copy(
                        endDate = DateTimeFormatState.of(
                            value = LocalDateTime(2026, 10, 15, 8, 30),
                            range = DateRangePresets.infinity(),
                            format = DateFormat.DateOnly.DateDdMmmYyyy,
                        ),
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun UnavailableLatestPreview() {
    PreviewContainer {
        val history = stubPerformanceTrendHistory()
        val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Density>().first()
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(
                history = persistentListOf(
                    history.first().copy(
                        metric = metric.copy(
                            current = null,
                            currentVsAveragePercentage = null,
                            status = PerformanceTrendStatusState.Empty,
                        ),
                    ),
                    history[1].copy(metric = metric),
                    history[2].copy(metric = metric.copy(current = 18f)),
                ),
                metricType = PerformanceMetricTypeState.Density,
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HistoryWithoutChartPreview() {
    PreviewContainer {
        val history = stubPerformanceTrendHistory()
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(
                history = persistentListOf(
                    history.first().copy(metric = stubEmptyPerformanceMetric()),
                    history[1],
                ),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun SameDayWorkoutsPreview() {
    PreviewContainer {
        val history = stubPerformanceTrendHistory()
        PerformanceTrendDetailsScreen(
            state = performanceDetailsPreviewState(
                history = persistentListOf(
                    history.first(),
                    history[1].copy(
                        endDate = DateTimeFormatState.of(
                            value = LocalDateTime(2026, 10, 15, 8, 30),
                            range = DateRangePresets.infinity(),
                            format = DateFormat.DateOnly.DateDdMmmYyyy,
                        ),
                    ),
                    history[2],
                ),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ExpandedExplanationPreview() {
    PreviewContainer {
        val state = performanceDetailsPreviewState()
        BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
            DialogRangeHeader(text = state.range.shortStartEnd())
            PerformanceTrendDetailsContent(
                modifier = Modifier.fillMaxWidth().weight(1f, false),
                state = state,
                isExpanded = true,
                onToggleExplanation = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun ExpandedSingleWorkoutExplanationPreview() {
    PreviewContainer {
        val entry = stubPerformanceTrendHistory().first()
        val metric = entry.metric as PerformanceMetricState.Volume
        val state = performanceDetailsPreviewState(
            history = persistentListOf(
                entry.copy(
                    metric = metric.copy(
                        average = null,
                        currentVsAveragePercentage = null,
                        status = PerformanceTrendStatusState.Stable,
                    ),
                ),
            ),
        )
        BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
            DialogRangeHeader(text = state.range.shortStartEnd())
            PerformanceTrendDetailsContent(
                modifier = Modifier.fillMaxWidth().weight(1f, false),
                state = state,
                isExpanded = true,
                onToggleExplanation = {},
            )
        }
    }
}
