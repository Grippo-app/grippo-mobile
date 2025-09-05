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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.design.components.chart.AreaChart
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.statistics.ChartCard
import com.grippo.design.components.tooltip.TooltipData
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

        if (state.exerciseVolumeData.first.items.isNotEmpty()) {
            item(key = "exercise_volume", span = { GridItemSpan(4) }) {
                val toolTip = remember(state.exerciseVolumeData.second) {
                    state.exerciseVolumeData.second?.let { instruction ->
                        TooltipData(
                            title = instruction.title,
                            description = instruction.description
                        )
                    }
                }

                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = AppTokens.strings.res(Res.string.chart_title_exercise_volume),
                    tooltip = toolTip,
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.exerciseVolumeData.first,
                        )
                    }
                )
            }
        }

        if (state.categoryDistributionData.first.slices.isNotEmpty()) {
            item(key = "category_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.categoryDistributionData.first
                )
            }
        }

        if (state.weightTypeDistributionData.first.slices.isNotEmpty()) {
            item(key = "weight_type_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.weightTypeDistributionData.first
                )
            }
        }

        if (state.experienceDistributionData.first.slices.isNotEmpty()) {
            item(key = "experience_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.experienceDistributionData.first
                )
            }
        }

        if (state.forceTypeDistributionData.first.slices.isNotEmpty()) {
            item(key = "force_type_distribution", span = { GridItemSpan(1) }) {
                PieChart(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    data = state.forceTypeDistributionData.first
                )
            }
        }

        if (state.muscleLoadData.first.items.isNotEmpty()) {
            item(key = "muscle_load", span = { GridItemSpan(4) }) {
                val toolTip = remember(state.muscleLoadData.second) {
                    state.muscleLoadData.second?.let { instruction ->
                        TooltipData(
                            title = instruction.title,
                            description = instruction.description
                        )
                    }
                }
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Muscle Load Distribution",
                    tooltip = toolTip,
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.muscleLoadData.first
                        )
                    }
                )
            }
        }

        if (state.percent1RMData.first.points.isNotEmpty()) {
            item(key = "percent_1rm", span = { GridItemSpan(4) }) {
                val toolTip = remember(state.percent1RMData.second) {
                    state.percent1RMData.second?.let { instruction ->
                        TooltipData(
                            title = instruction.title,
                            description = instruction.description
                        )
                    }
                }
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "Intra progression",
                    tooltip = toolTip,
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.percent1RMData.first
                        )
                    }
                )
            }
        }

        if (state.stimulusData.first.points.isNotEmpty()) {
            item(key = "stimulus", span = { GridItemSpan(4) }) {
                val toolTip = remember(state.stimulusData.second) {
                    state.stimulusData.second?.let { instruction ->
                        TooltipData(
                            title = instruction.title,
                            description = instruction.description
                        )
                    }
                }
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "Stimulus",
                    tooltip = toolTip,
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.stimulusData.first
                        )
                    }
                )
            }
        }

        if (state.estimated1RMData.first.items.isNotEmpty()) {
            item(key = "estimated_1rm", span = { GridItemSpan(4) }) {
                val toolTip = remember(state.estimated1RMData.second) {
                    state.estimated1RMData.second?.let { instruction ->
                        TooltipData(
                            title = instruction.title,
                            description = instruction.description
                        )
                    }
                }
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "Estimated 1RM",
                    tooltip = toolTip,
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.estimated1RMData.first,
                        )
                    }
                )
            }
        }
    }
}
