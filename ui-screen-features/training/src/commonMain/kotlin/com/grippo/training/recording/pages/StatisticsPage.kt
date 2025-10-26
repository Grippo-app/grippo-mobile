package com.grippo.training.recording.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.stubTraining
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
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscles
import com.grippo.design.resources.provider.no_data_yet
import com.grippo.design.resources.provider.trends
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
    AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        },
        targetState = state.exercises.isEmpty()
    ) {
        when (it) {
            true -> ScreenPlaceholder(
                modifier = Modifier.fillMaxSize(),
                text = AppTokens.strings.res(Res.string.no_data_yet),
            )

            false -> LazyColumn(
                modifier = modifier,
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
                            ?.takeIf { t -> t.value != null }
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
                            ?.takeIf { t -> t.value != null }
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
                            ?.takeIf { t -> t.value != null }
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
                    ?.takeIf { t -> t.points.isNotEmpty() }
                    ?.let { data ->
                        item(key = "exercise_volume") {
                            MetricBarChart(
                                modifier = Modifier
                                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
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
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                    ) {
                        state.categoryDistribution
                            ?.takeIf { t -> t.slices.isNotEmpty() }
                            ?.let { data ->
                                DistributionPieChart(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    value = data
                                )
                            }

                        state.weightTypeDistribution
                            ?.takeIf { t -> t.slices.isNotEmpty() }
                            ?.let { data ->
                                DistributionPieChart(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    value = data
                                )
                            }

                        state.forceTypeDistribution
                            ?.takeIf { t -> t.slices.isNotEmpty() }
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
                    ?.takeIf { t -> t.perGroup.entries.isNotEmpty() }
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
