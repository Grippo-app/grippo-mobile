package com.grippo.performance.trend.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.grippo.core.state.metrics.performance.stubPerformanceTrendHistory
import com.grippo.design.components.metrics.performance.PerformanceMetricCard
import com.grippo.design.components.metrics.performance.PerformanceTrendHistoryCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.history
import com.grippo.design.resources.provider.performance_trend
import com.grippo.design.resources.provider.value_performance_trend
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun PerformanceTrendDetailsScreen(
    state: PerformanceTrendDetailsDialogState,
    loaders: ImmutableSet<PerformanceTrendDetailsLoader>,
    contract: PerformanceTrendDetailsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    val metricLabel = state.metricType.label()

    val title = state.range.label()?.text()?.let {
        AppTokens.strings.res(Res.string.value_performance_trend, it, metricLabel)
    } ?: AppTokens.strings.res(Res.string.performance_trend, metricLabel)

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = state.range.display,
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        item(key = "info") {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = state.metricType.description(),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )
        }

        item(key = "chart") {
            PerformanceTrendHistoryCard(
                modifier = Modifier.fillMaxWidth(),
                history = state.history,
            )
        }

        if (state.history.isNotEmpty()) {
            item(key = "history_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.history),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
        }

        itemsIndexed(
            items = state.history,
            key = { index, _ -> "history_$index" }
        ) { index, entry ->
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                    text = entry.endDate.display,
                    style = style,
                    color = color
                )

                PerformanceMetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    metric = entry.metric
                )
            }
        }

        item("bottom_space") {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PerformanceTrendDetailsScreen(
            state = PerformanceTrendDetailsDialogState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                metricType = PerformanceMetricTypeState.Volume,
                history = stubPerformanceTrendHistory(),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty
        )
    }
}
