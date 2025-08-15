package com.grippo.home.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            item {
                ChartCard(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    content = { AreaChart(modifier = Modifier.fillMaxSize()) }
                )
            }
            item {
                ChartCard(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.6f),
                    content = { BarChart(modifier = Modifier.fillMaxSize()) })
            }
            item {
                ChartCard(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
                    content = { HeatmapChart(modifier = Modifier.fillMaxSize().clipToBounds()) })
            }
            item {
                ChartCard(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
                    content = { ProgressChart(modifier = Modifier.fillMaxSize()) })
            }
            item {
                ChartCard(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    content = { RadarChart(modifier = Modifier.fillMaxSize()) })
            }
            item {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    content = { Sparkline(modifier = Modifier.fillMaxWidth().height(120.dp)) })
            }
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