package com.grippo.home.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.chart.DistributionPieChart
import com.grippo.design.components.chart.MetricBarChart
import com.grippo.design.components.chart.MuscleHeatmapChart
import com.grippo.design.components.chart.MuscleLoadChart
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.datetime.PeriodPicker
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.statistics
import com.grippo.toolkit.date.utils.DateFormat
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
                    format = DateFormat.DATE_DD_DOT_MM,
                    onClick = contract::onSelectPeriod
                )
            }
        }
    )

    if (loaders.contains(HomeStatisticsLoader.Charts)) {
        Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        return@BaseComposeScreen
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(vertical = AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
    ) {

        item(key = "summary_chips") {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                state
                    .totalMetrics
                    ?.volume
                    ?.takeIf { it.value != null }
                    ?.let { data ->
                        VolumeChip(
                            modifier = Modifier.weight(1f),
                            value = data,
                            style = VolumeChipStyle.SHORT,
                            size = ChipSize.Medium
                        )
                    }

                state
                    .totalMetrics
                    ?.repetitions
                    ?.takeIf { it.value != null }
                    ?.let { data ->
                        RepetitionsChip(
                            modifier = Modifier.weight(1f),
                            value = data,
                            style = RepetitionsChipStyle.SHORT,
                            size = ChipSize.Medium
                        )
                    }

                state
                    .totalMetrics
                    ?.intensity
                    ?.takeIf { it.value != null }
                    ?.let { data ->
                        IntensityChip(
                            modifier = Modifier.weight(1f),
                            value = data,
                            style = IntensityChipStyle.SHORT,
                            size = ChipSize.Medium
                        )
                    }
            }
        }

        state.exerciseVolume
            ?.takeIf { it.points.isNotEmpty() }
            ?.let { data ->
                item(key = "exercise_volume") {
                    MetricBarChart(
                        modifier = Modifier
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                            .fillMaxWidth()
                            .aspectRatio(1.4f),
                        value = data,
                    )
                }
            }

        item(key = "trends_divider") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppTokens.colors.divider.default
                )

                Text(
                    text = "Trends",
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.tertiary,
                )

                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppTokens.colors.divider.default
                )
            }
        }

        item(key = "distribution") {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                state.categoryDistribution
                    ?.takeIf { it.slices.isNotEmpty() }
                    ?.let { data ->
                        DistributionPieChart(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            value = data
                        )
                    }

                state.weightTypeDistribution
                    ?.takeIf { it.slices.isNotEmpty() }
                    ?.let { data ->
                        DistributionPieChart(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            value = data
                        )
                    }

                state.forceTypeDistribution
                    ?.takeIf { it.slices.isNotEmpty() }
                    ?.let { data ->
                        DistributionPieChart(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            value = data
                        )
                    }
            }
        }

        item(key = "muscles_divider") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppTokens.colors.divider.default
                )

                Text(
                    text = "Muscles",
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.tertiary,
                )

                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppTokens.colors.divider.default
                )
            }
        }

        state.muscleLoad
            ?.takeIf { it.perGroup.entries.isNotEmpty() }
            ?.let { summary ->
                item(key = "muscle_load") {
                    MuscleLoadChart(
                        modifier = Modifier
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                            .fillMaxWidth(),
                        value = summary,
                    )
                }
            }

        state.temporalHeatmap
            ?.takeIf { it.values01.isNotEmpty() }
            ?.let { data ->
                item(key = "temporal_heatmap") {
                    MuscleHeatmapChart(
                        modifier = Modifier
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                            .fillMaxWidth(),
                        value = data,
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
