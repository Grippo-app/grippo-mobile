package com.grippo.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.trainings.stubTraining
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
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscles
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.trends
import com.grippo.design.resources.provider.value_statistics
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun StatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<StatisticsLoader>,
    contract: StatisticsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = when (val mode = state.mode) {
                is StatisticsMode.Exercises -> AppTokens.strings.res(
                    Res.string.statistics
                )

                is StatisticsMode.Trainings -> mode.range.label()?.let {
                    AppTokens.strings.res(Res.string.value_statistics, it)
                } ?: AppTokens.strings.res(
                    Res.string.statistics
                )
            },
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        when (val mode = state.mode) {
            is StatisticsMode.Exercises -> {

            }

            is StatisticsMode.Trainings -> {
                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = mode.range.formatted(),
                    style = AppTokens.typography.b14Semi(),
                    color = AppTokens.colors.text.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        if (loaders.contains(StatisticsLoader.Charts)) {
            Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = AppTokens.dp.dialog.horizontalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
            ) {
                item(key = "summary_chips") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .fillMaxWidth()
                                    .aspectRatio(1.7f),
                                value = data,
                            )
                        }
                    }

                item(key = "trends_spliter") {
                    ContentSpliter(
                        text = AppTokens.strings.res(Res.string.trends)
                    )
                }

                item(key = "distribution") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                item(key = "muscles_spliter") {
                    ContentSpliter(
                        text = AppTokens.strings.res(Res.string.muscles)
                    )
                }

                state.muscleLoad
                    ?.takeIf { it.perGroup.entries.isNotEmpty() }
                    ?.let { summary ->
                        item(key = "muscle_load") {
                            MuscleLoadChart(
                                modifier = Modifier.fillMaxWidth(),
                                value = summary,
                            )
                        }
                    }

                state.temporalHeatmap
                    ?.takeIf { it.values01.isNotEmpty() }
                    ?.let { data ->
                        item(key = "temporal_heatmap") {
                            MuscleHeatmapChart(
                                modifier = Modifier.fillMaxWidth(),
                                value = data,
                            )
                        }
                    }
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(
                    trainings = persistentListOf(stubTraining(), stubTraining(), stubTraining()),
                    range = DateTimeUtils.thisWeek()
                )
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty
        )
    }
}
