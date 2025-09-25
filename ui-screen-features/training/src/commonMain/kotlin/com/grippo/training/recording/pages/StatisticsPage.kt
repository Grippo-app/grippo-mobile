package com.grippo.training.recording.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.muscle.MuscleLoad
import com.grippo.design.components.statistics.ChartCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.chart_title_exercise_volume
import com.grippo.design.resources.provider.chart_title_muscle_load
import com.grippo.design.resources.provider.icons.Reports
import com.grippo.design.resources.provider.no_data_yet
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
                            muscleImages = state.muscleLoadImages,
                        )
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
