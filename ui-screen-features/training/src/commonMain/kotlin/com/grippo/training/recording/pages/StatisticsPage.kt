package com.grippo.training.recording.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.grippo.design.components.statistics.ChartCard
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.chart_title_exercise_volume
import com.grippo.training.recording.TrainingRecordingContract
import com.grippo.training.recording.TrainingRecordingState

@Composable
internal fun StatisticsPage(
    modifier: Modifier = Modifier,
    state: TrainingRecordingState,
    contract: TrainingRecordingContract
) {
    LazyVerticalGrid(
        modifier = modifier,
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
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.muscleLoadData
                        )
                    }
                )
            }
        }
    }
}

