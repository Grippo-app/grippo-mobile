package com.grippo.performance.trend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.stubPerformanceTrendHistory
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.PerformanceTrendCard
import com.grippo.design.components.metrics.PerformanceTrendChartSection
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.performance_trend
import com.grippo.design.resources.provider.value_performance_trend
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun PerformanceTrendScreen(
    state: PerformanceTrendDialogState,
    loaders: ImmutableSet<PerformanceTrendLoader>,
    contract: PerformanceTrendContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        val metricLabel = state.metricType.label()

        val title = state.range.label()?.let {
            AppTokens.strings.res(Res.string.value_performance_trend, it, metricLabel)
        } ?: AppTokens.strings.res(Res.string.performance_trend, metricLabel)

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.range.formatted(),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        if (loaders.contains(PerformanceTrendLoader.Content)) {
            Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                item(key = "chart") {
                    PerformanceTrendChartSection(
                        modifier = Modifier.fillMaxWidth(),
                        history = state.history,
                    )
                }

                itemsIndexed(
                    items = state.history,
                    key = { index, _ -> "history_$index" }
                ) { index, entry ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
                    ) {
                        val formattedDate = DateCompose.rememberFormat(
                            entry.range.to,
                            DateFormat.DateOnly.DateMmmDdYyyy
                        )

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
                            text = formattedDate,
                            style = style,
                            color = color
                        )

                        PerformanceTrendCard(
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
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PerformanceTrendScreen(
            state = PerformanceTrendDialogState(
                range = DateTimeUtils.trailingWeek(),
                metricType = PerformanceMetricTypeState.Volume,
                history = stubPerformanceTrendHistory(),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendContract.Empty
        )
    }
}
