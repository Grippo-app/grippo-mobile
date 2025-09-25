package com.grippo.home.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.HeatmapChart
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.datetime.PeriodPicker
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.statistics.ChartCard
import com.grippo.design.components.statistics.MuscleLoad
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.chart_title_exercise_volume
import com.grippo.design.resources.provider.chart_title_muscle_heap
import com.grippo.design.resources.provider.chart_title_muscle_load
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

    if (loaders.contains(HomeStatisticsLoader.Charts)) {
        Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        return@BaseComposeScreen
    }

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {

        item(key = "summary_chips_volume", span = { GridItemSpan(1) }) {
            VolumeChip(
                value = state.totalVolume,
                style = VolumeChipStyle.SHORT,
                size = ChipSize.Medium
            )
        }
        item(key = "summary_chips_repeat", span = { GridItemSpan(1) }) {
            RepetitionsChip(
                value = state.totalRepetitions,
                style = RepetitionsChipStyle.SHORT,
                size = ChipSize.Medium
            )
        }
        item(key = "summary_chips_intensity", span = { GridItemSpan(1) }) {
            IntensityChip(
                value = state.averageIntensity,
                style = IntensityChipStyle.SHORT,
                size = ChipSize.Medium
            )
        }

        if (state.exerciseVolumeData.items.isNotEmpty()) {
            item(key = "exercise_volume", span = { GridItemSpan(3) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = AppTokens.strings.res(Res.string.chart_title_exercise_volume),
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.exerciseVolumeData,
                        )
                    }
                )
            }
        }

        if (state.categoryDistributionData.slices.isNotEmpty()) {
            item(key = "category_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.categoryDistributionData
                )
            }
        }

        if (state.weightTypeDistributionData.slices.isNotEmpty()) {
            item(key = "weight_type_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.weightTypeDistributionData
                )
            }
        }

        if (state.forceTypeDistributionData.slices.isNotEmpty()) {
            item(key = "force_type_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.forceTypeDistributionData
                )
            }
        }

        if (state.muscleLoadData.items.isNotEmpty()) {
            item(key = "muscle_load", span = { GridItemSpan(3) }) {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = AppTokens.strings.res(Res.string.chart_title_muscle_load),
                    content = {
                        MuscleLoad(
                            modifier = Modifier.fillMaxWidth(),
                            chartData = state.muscleLoadData,
                            muscleBreakdown = state.muscleLoadMuscles,
                        )
                    }
                )
            }
        }

        if (state.temporalHeatmapData.values01.isNotEmpty()) {
            item(key = "temporal_heatmap", span = { GridItemSpan(3) }) {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = AppTokens.strings.res(Res.string.chart_title_muscle_heap),
                    content = {
                        HeatmapChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.temporalHeatmapData,
                        )
                    }
                )
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
