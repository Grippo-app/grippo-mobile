package com.grippo.training.recording.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.calculation.models.toColorSources
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.statistics.ChartCard
import com.grippo.design.components.tooltip.TooltipData
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.chart_title_exercise_volume
import com.grippo.design.resources.provider.icons.Reports
import com.grippo.design.resources.provider.no_data_yet
import com.grippo.state.trainings.stubTraining
import com.grippo.calculation.muscle.MuscleEngine
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
        Placeholder(
            modifier = modifier
        )
    } else LazyVerticalGrid(
        modifier = modifier,
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

        if (state.exerciseVolumeData.first.items.isNotEmpty()) {
            item(key = "exercise_volume", span = { GridItemSpan(3) }) {
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

        if (state.muscleLoadData.first.items.isNotEmpty()) {
            item(key = "muscle_load", span = { GridItemSpan(3) }) {
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
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                        ) {
                            val sources = state.muscleLoadBreakdown?.toColorSources()
                            val preset = sources?.takeIf { it.isNotEmpty() }?.let {
                                MuscleEngine.generatePreset(it)
                            }
                            val images = preset?.let { MuscleEngine.generateImages(it) }

                            images?.let { (front, back) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        modifier = Modifier
                                            .weight(1f),
                                        imageVector = front,
                                        contentDescription = null
                                    )

                                    Image(
                                        modifier = Modifier
                                            .weight(1f),
                                        imageVector = back,
                                        contentDescription = null
                                    )
                                }
                            }

                            ProgressChart(
                                modifier = Modifier.fillMaxWidth(),
                                data = state.muscleLoadData.first
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .alpha(0.2f)
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Icon(
            modifier = Modifier
                .background(AppTokens.colors.background.card, CircleShape)
                .size(200.dp)
                .padding(24.dp),
            imageVector = AppTokens.icons.Reports,
            contentDescription = null,
            tint = AppTokens.colors.icon.primary
        )

        Text(
            text = AppTokens.strings.res(Res.string.no_data_yet),
            textAlign = TextAlign.Center,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun StatisticsPagePreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
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
                exercises = persistentListOf(),
                tab = RecordingTab.Stats
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}
