package com.grippo.training.recording.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.chart.ChartCard
import com.grippo.design.components.chart.DistributionPieChart
import com.grippo.design.components.chart.MetricBarChart
import com.grippo.design.components.chart.MuscleLoadChart
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.placeholder.ScreenPlaceholder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.chart_title_exercise_volume
import com.grippo.design.resources.provider.chart_title_muscle_load
import com.grippo.design.resources.provider.no_data_yet
import com.grippo.state.stage.StageState
import com.grippo.state.trainings.stubTraining
import com.grippo.training.recording.RecordingTab
import com.grippo.training.recording.TrainingRecordingContract
import com.grippo.training.recording.TrainingRecordingScreen
import com.grippo.training.recording.TrainingRecordingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun StatisticsPage(
    modifier: Modifier = Modifier,
    state: TrainingRecordingState,
    contract: TrainingRecordingContract
) {

    if (state.exercises.isEmpty()) {
        ScreenPlaceholder(
            modifier = modifier,
            text = AppTokens.strings.res(Res.string.no_data_yet),
        )
    } else LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {

        state.totalMetrics
            ?.volume
            ?.takeIf { it.value != null }
            ?.let { data ->
                item(key = "summary_chips_volume", span = { GridItemSpan(1) }) {
                    VolumeChip(
                        value = data,
                        style = VolumeChipStyle.SHORT,
                        size = ChipSize.Medium
                    )
                }
            }

        state.totalMetrics
            ?.repetitions
            ?.takeIf { it.value != null }
            ?.let { data ->
                item(key = "summary_chips_repeat", span = { GridItemSpan(1) }) {
                    RepetitionsChip(
                        value = data,
                        style = RepetitionsChipStyle.SHORT,
                        size = ChipSize.Medium
                    )
                }
            }

        state.totalMetrics
            ?.intensity
            ?.takeIf { it.value != null }
            ?.let { data ->
                item(key = "summary_chips_intensity", span = { GridItemSpan(1) }) {
                    IntensityChip(
                        value = data,
                        style = IntensityChipStyle.SHORT,
                        size = ChipSize.Medium
                    )
                }
            }

        state.exerciseVolume
            ?.takeIf { it.points.isNotEmpty() }
            ?.let { data ->
                item(key = "exercise_volume", span = { GridItemSpan(3) }) {
                    ChartCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f),
                        title = AppTokens.strings.res(Res.string.chart_title_exercise_volume),
                        content = {
                            MetricBarChart(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                value = data,
                            )
                        }
                    )
                }
            }

        state.categoryDistribution
            ?.takeIf { it.slices.isNotEmpty() }
            ?.let { data ->
                item(key = "category_distribution", span = { GridItemSpan(1) }) {
                    DistributionPieChart(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        value = data
                    )
                }
            }

        state.weightTypeDistribution
            ?.takeIf { it.slices.isNotEmpty() }
            ?.let { data ->
                item(key = "weight_type_distribution", span = { GridItemSpan(1) }) {
                    DistributionPieChart(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        value = data
                    )
                }
            }

        state.forceTypeDistribution
            ?.takeIf { it.slices.isNotEmpty() }
            ?.let { data ->
                item(key = "force_type_distribution", span = { GridItemSpan(1) }) {
                    DistributionPieChart(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        value = data
                    )
                }
            }

        state.muscleLoad
            ?.takeIf { it.perGroup.entries.isNotEmpty() }
            ?.let { summary ->
                item(key = "muscle_load", span = { GridItemSpan(3) }) {
                    ChartCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = AppTokens.strings.res(Res.string.chart_title_muscle_load),
                        content = {
                            MuscleLoadChart(
                                modifier = Modifier.fillMaxWidth(),
                                value = summary,
                            )
                        }
                    )
                }
            }
    }
}

@AppPreview
@Composable
private fun StatisticsPagePreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = stubTraining().exercises,
                tab = RecordingTab.Stats
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun StatisticsPageEmptyPreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = persistentListOf(),
                tab = RecordingTab.Stats
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}
