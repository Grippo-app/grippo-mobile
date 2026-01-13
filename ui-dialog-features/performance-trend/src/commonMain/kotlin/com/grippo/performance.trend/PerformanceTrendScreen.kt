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
import com.grippo.design.resources.provider.density
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.intensity_chip
import com.grippo.design.resources.provider.performance_trend
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.value_performance_trend
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
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
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.secondary,
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
                contentPadding = PaddingValues(
                    horizontal = AppTokens.dp.dialog.horizontalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                item(key = "chart") {
                    PerformanceTrendChartSection(
                        modifier = Modifier.fillMaxWidth(),
                        chartPoints = state.chartPoints,
                    )
                }

                itemsIndexed(
                    items = state.history,
                    key = { index, _ -> "history_$index" }
                ) { _, entry ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(
                            AppTokens.dp.contentPadding.subContent
                        )
                    ) {
                        val formattedDate = DateCompose.rememberFormat(
                            entry.range.to,
                            DateFormat.DateOnly.DateMmmDdYyyy
                        )

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = formattedDate,
                            style = AppTokens.typography.b12Med(),
                            color = AppTokens.colors.text.tertiary
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

@Composable
private fun PerformanceMetricTypeState.label(): String {
    return when (this) {
        PerformanceMetricTypeState.Duration -> AppTokens.strings.res(Res.string.duration)
        PerformanceMetricTypeState.Volume -> AppTokens.strings.res(Res.string.volume)
        PerformanceMetricTypeState.Density -> AppTokens.strings.res(Res.string.density)
        PerformanceMetricTypeState.Repetitions -> AppTokens.strings.res(Res.string.repetitions)
        PerformanceMetricTypeState.Intensity -> AppTokens.strings.res(Res.string.intensity_chip)
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
                chartPoints = persistentListOf(4f, 6f, 5f, 8f, 9f, 7f),
                history = stubPerformanceTrendHistory(),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendContract.Empty
        )
    }
}
