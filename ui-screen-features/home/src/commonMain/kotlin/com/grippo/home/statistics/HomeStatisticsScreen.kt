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
import com.grippo.design.components.chart.AreaChart
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chart.ProgressChart
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
import com.grippo.design.components.tooltip.TooltipData
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.chart_title_exercise_volume
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_trainings
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_trainings
import com.grippo.design.resources.provider.tooltip_muscle_load_description_trainings
import com.grippo.design.resources.provider.tooltip_muscle_load_title_trainings
import com.grippo.design.resources.provider.tooltip_percent1rm_description_trainings
import com.grippo.design.resources.provider.tooltip_percent1rm_title_trainings
import com.grippo.design.resources.provider.tooltip_stimulus_description_trainings
import com.grippo.design.resources.provider.tooltip_stimulus_title_trainings
import com.grippo.design.resources.provider.tooltip_volume_description_trainings
import com.grippo.design.resources.provider.tooltip_volume_title_trainings
import com.grippo.state.formatters.UiText
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

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {

        item(key = "summary_chips", span = { GridItemSpan(4) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                VolumeChip(
                    modifier = Modifier.weight(1f),
                    value = state.totalVolume,
                    style = VolumeChipStyle.SHORT
                )

                RepetitionsChip(
                    modifier = Modifier.weight(1f),
                    value = state.totalRepetitions,
                    style = RepetitionsChipStyle.SHORT
                )

                IntensityChip(
                    modifier = Modifier.weight(1f),
                    value = state.averageIntensity,
                    style = IntensityChipStyle.SHORT
                )
            }
        }

        if (state.exerciseVolumeData.items.isNotEmpty()) {
            item(key = "exercise_volume", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = AppTokens.strings.res(Res.string.chart_title_exercise_volume),
                    tooltip = TooltipData(
                        title = UiText.Res(Res.string.tooltip_volume_title_trainings),
                        description = UiText.Res(Res.string.tooltip_volume_description_trainings),
                    ),
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.exerciseVolumeData,
                            xAxisLabelStyle = XAxisLabelStyle.SHOW_ALL
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

        if (state.experienceDistributionData.slices.isNotEmpty()) {
            item(key = "experience_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.experienceDistributionData
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
            item(key = "muscle_load", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Muscle Load Distribution",
                    tooltip = TooltipData(
                        title = UiText.Res(Res.string.tooltip_muscle_load_title_trainings),
                        description = UiText.Res(Res.string.tooltip_muscle_load_description_trainings),
                    ),
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.muscleLoadData
                        )
                    }
                )
            }
        }

        if (state.intraProgressionData.points.isNotEmpty()) {
            item(key = "intra_progression", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "Intra progression",
                    tooltip = TooltipData(
                        title = UiText.Res(Res.string.tooltip_percent1rm_title_trainings),
                        description = UiText.Res(Res.string.tooltip_percent1rm_description_trainings),
                    ),
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.intraProgressionData
                        )
                    }
                )
            }
        }

        if (state.intraProgressionData.points.isNotEmpty()) {
            item(key = "intra_progression_percent1rm", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "Intra progression (%1RM)",
                    tooltip = TooltipData(
                        title = UiText.Res(Res.string.tooltip_percent1rm_title_trainings),
                        description = UiText.Res(Res.string.tooltip_percent1rm_description_trainings),
                    ),
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.intraProgressionData
                        )
                    }
                )
            }
        }

        if (state.stimulusData.points.isNotEmpty()) {
            item(key = "stimulus", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "Stimulus",
                    tooltip = TooltipData(
                        title = UiText.Res(Res.string.tooltip_stimulus_title_trainings),
                        description = UiText.Res(Res.string.tooltip_stimulus_description_trainings),
                    ),
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.stimulusData
                        )
                    }
                )
            }
        }

        if (state.estimated1RMData.items.isNotEmpty()) {
            item(key = "estimated_1rm", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "Estimated 1RM",
                    tooltip = TooltipData(
                        title = UiText.Res(Res.string.tooltip_estimated1rm_title_trainings),
                        description = UiText.Res(Res.string.tooltip_estimated1rm_description_trainings),
                    ),
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.estimated1RMData,
                            xAxisLabelStyle = XAxisLabelStyle.SHOW_ALL
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