package com.grippo.home.statistics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.grippo.design.components.chart.XAxisLabelStyle
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeriodPicker(
                    value = state.period,
                    format = DateFormat.DATE_DD_MMM,
                    onClick = contract::onSelectPeriod
                )
            }
        }
    )

    LazyVerticalStaggeredGrid(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalItemSpacing = AppTokens.dp.contentPadding.content,
        contentPadding = PaddingValues(vertical = AppTokens.dp.contentPadding.content)
    ) {

        item(key = "chips", span = StaggeredGridItemSpan.FullLine) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                VolumeChip(
                    value = state.volume,
                    style = VolumeChipStyle.LONG
                )

                RepetitionsChip(
                    value = state.repetitions,
                    style = RepetitionsChipStyle.LONG
                )

                IntensityChip(
                    value = state.intensity,
                    style = IntensityChipStyle.LONG
                )
            }
        }

        item(key = "area_chart", span = StaggeredGridItemSpan.FullLine) {
            ChartCard(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth()
                    .aspectRatio(1.8f),
                title = AppTokens.strings.res(Res.string.statistics),
                content = {
                    AreaChart(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        data = state.areaData
                    )
                }
            )
        }

        item(key = "bar_chart") {
            ChartCard(
                modifier = Modifier
                    .padding(start = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                title = "Weekly Volume",
                content = {
                    BarChart(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        data = state.barData,
                        xAxisLabelStyle = XAxisLabelStyle.ADAPTIVE
                    )
                }
            )
        }

        item(key = "sparkline_chart") {
            ChartCard(
                modifier = Modifier
                    .padding(end = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                title = "Trend",
                content = {
                    Sparkline(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        data = state.sparklineData
                    )
                }
            )
        }

        item(key = "heatmap_chart", span = StaggeredGridItemSpan.FullLine) {
            ChartCard(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth()
                    .aspectRatio(1.5f),
                title = "Activity Heatmap",
                content = {
                    HeatmapChart(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        data = state.heatmapData
                    )
                }
            )
        }

        item(key = "radar_chart") {
            ChartCard(
                modifier = Modifier
                    .padding(start = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                title = "Muscle Balance",
                content = {
                    RadarChart(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        data = state.radarData
                    )
                }
            )
        }

        item(key = "progress_chart") {
            ChartCard(
                modifier = Modifier
                    .padding(end = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                title = "Progress",
                content = {
                    ProgressChart(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        data = state.progressData
                    )
                }
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