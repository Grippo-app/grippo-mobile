package com.grippo.home.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.chart.AreaChart
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.HeatmapChart
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.components.chart.RadarChart
import com.grippo.design.components.chart.Sparkline
import com.grippo.design.components.datetime.PeriodPicker
import com.grippo.design.components.statistics.ChartCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.statistics
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun HomeStatisticsScreen(
    state: HomeStatisticsState,
    loaders: ImmutableSet<HomeStatisticsLoader>,
    contract: HomeStatisticsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.statistics),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = AppTokens.dp.contentPadding.content,
                        start = AppTokens.dp.screen.horizontalPadding,
                        end = AppTokens.dp.screen.horizontalPadding
                    ),
            ) {
                PeriodPicker(
                    value = state.period,
                    format = DateFormat.MMM_dd,
                    onClick = contract::selectPeriod
                )
            }
        }
    )

    LazyVerticalStaggeredGrid(
        modifier = Modifier.fillMaxWidth()
            .weight(1f),
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalItemSpacing = AppTokens.dp.contentPadding.content,
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        )
    ) {
        // Hero area chart — full width
        item(key = "area_chart", span = StaggeredGridItemSpan.FullLine) {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.8f),
                title = AppTokens.strings.res(Res.string.statistics),
                content = { AreaChart(modifier = Modifier.fillMaxWidth().weight(1f)) }
            )
        }

        // Bar and Sparkline — side by side
        item(key = "bar_chart") {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                title = "Weekly Volume",
                content = { BarChart(modifier = Modifier.fillMaxWidth().weight(1f)) }
            )
        }
        item(key = "sparkline_chart") {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                title = "Trend",
                content = { Sparkline(modifier = Modifier.fillMaxWidth().weight(1f)) }
            )
        }

        item(key = "heatmap_chart", span = StaggeredGridItemSpan.FullLine) {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                title = "Activity Heatmap",
                content = { HeatmapChart(modifier = Modifier.fillMaxWidth().weight(1f)) }
            )
        }

        item(key = "radar_chart") {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                title = "Muscle Balance",
                content = { RadarChart(modifier = Modifier.fillMaxWidth().weight(1f)) }
            )
        }
        item(key = "progress_chart") {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                title = "Progress",
                content = { ProgressChart(modifier = Modifier.fillMaxWidth().weight(1f)) }
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        HomeStatisticsScreen(
            state = HomeStatisticsState(),
            loaders = persistentSetOf(),
            contract = HomeStatisticsContract.Empty
        )
    }
}