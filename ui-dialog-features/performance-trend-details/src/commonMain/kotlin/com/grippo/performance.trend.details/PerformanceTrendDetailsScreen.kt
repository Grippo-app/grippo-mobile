package com.grippo.performance.trend.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.stubPerformanceTrendHistory
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.metrics.performance.PerformanceMetricCard
import com.grippo.design.components.metrics.performance.PerformanceTrendHistoryCard
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.history
import com.grippo.design.resources.provider.performance_history_scope
import com.grippo.design.resources.provider.performance_history_empty
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun PerformanceTrendDetailsScreen(
    state: PerformanceTrendDetailsDialogState,
    loaders: ImmutableSet<PerformanceTrendDetailsLoader>,
    contract: PerformanceTrendDetailsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        text = state.range.short(),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    ContentDivider()

    val listState = rememberAnchoredLazyListState(
        behavior = AnchorScrollBehavior.Animated
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = AppTokens.dp.dialog.horizontalPadding,
            end = AppTokens.dp.dialog.horizontalPadding,
            top = AppTokens.dp.contentPadding.content,
            bottom = AppTokens.dp.dialog.bottom,
        ),
    ) {
        item(key = "info") {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "${state.metricType.description()}\n\n${AppTokens.strings.res(Res.string.performance_history_scope)}",
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )
        }

        if (state.history.isEmpty()) {
            item(key = "empty") {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = AppTokens.dp.contentPadding.content),
                    text = AppTokens.strings.res(Res.string.performance_history_empty),
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        item(key = "chart") {
            PerformanceTrendHistoryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTokens.dp.contentPadding.content),
                history = state.history,
            )
        }

        if (state.history.isNotEmpty()) {
            item(key = "history_header") {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.block),
                    text = AppTokens.strings.res(Res.string.history),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
        }

        itemsIndexed(
            items = state.history,
            key = { _, entry -> entry.trainingId }
        ) { index, entry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTokens.dp.contentPadding.content),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
            ) {
                val style = if (index == 0) {
                    AppTokens.typography.h5()
                } else {
                    AppTokens.typography.b13Med()
                }

                val color = if (index == 0) {
                    AppTokens.colors.text.primary
                } else {
                    AppTokens.colors.text.secondary
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = entry.endDate.short(),
                    style = style,
                    color = color
                )

                PerformanceMetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    metric = entry.metric
                )
            }
        }

    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PerformanceTrendDetailsScreen(
            state = PerformanceTrendDetailsDialogState(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(2026, 10, 11, 0, 0),
                    to = LocalDateTime(2026, 10, 17, 23, 59),
                ),
                metricType = PerformanceMetricTypeState.Volume,
                history = stubPerformanceTrendHistory(),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun WithoutHistoryPreview() {
    PreviewContainer {
        PerformanceTrendDetailsScreen(
            state = PerformanceTrendDetailsDialogState(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(2026, 10, 11, 0, 0),
                    to = LocalDateTime(2026, 10, 17, 23, 59),
                ),
                metricType = PerformanceMetricTypeState.Volume,
            ),
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
            state = PerformanceTrendDetailsDialogState(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(2026, 10, 11, 0, 0),
                    to = LocalDateTime(2026, 10, 17, 23, 59),
                ),
                metricType = PerformanceMetricTypeState.Volume,
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
