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
import com.grippo.design.components.chart.AreaChart
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.components.chart.Sparkline
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

        if (state.intraWorkoutProgressionData.points.isNotEmpty()) {
            item(key = "intra_workout_progression", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f),
                    title = "Performance Throughout Workout",
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.intraWorkoutProgressionData
                        )
                    }
                )
            }
        }

        if (state.intensityDistributionData.items.isNotEmpty()) {
            item(key = "intensity_distribution", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.8f),
                    title = "Intensity Distribution",
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.intensityDistributionData,
                            xAxisLabelStyle = XAxisLabelStyle.SHOW_ALL
                        )
                    }
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

        if (state.workoutEfficiencyData.items.isNotEmpty()) {
            item(key = "workout_efficiency", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Workout Efficiency",
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.workoutEfficiencyData
                        )
                    }
                )
            }
        }

        if (state.rpeAnalysisData.items.isNotEmpty()) {
            item(key = "rpe_analysis", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    title = "RPE Analysis (Perceived Exertion)",
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.rpeAnalysisData,
                            xAxisLabelStyle = XAxisLabelStyle.SHOW_ALL
                        )
                    }
                )
            }
        }

        if (state.repRangeDistributionData.slices.isNotEmpty()) {
            item(key = "rep_range_distribution", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    title = "Rep Range Focus",
                    content = {
                        PieChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.repRangeDistributionData
                        )
                    }
                )
            }
        }

        if (state.techniqueQualityData.points.isNotEmpty()) {
            item(key = "technique_quality", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f),
                    title = "Technique Consistency",
                    content = {
                        Sparkline(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.techniqueQualityData
                        )
                    }
                )
            }
        }

        if (state.timeUnderTensionData.items.isNotEmpty()) {
            item(key = "time_under_tension", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Time Under Tension",
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.timeUnderTensionData
                        )
                    }
                )
            }
        }

        if (state.energyExpenditureData.items.isNotEmpty()) {
            item(key = "energy_expenditure", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Energy Expenditure",
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.energyExpenditureData
                        )
                    }
                )
            }
        }

        if (state.loadOverTimeData.points.isNotEmpty()) {
            item(key = "load_over_time", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f),
                    title = "Load Over Time",
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.loadOverTimeData
                        )
                    }
                )
            }
        }

        if (state.fatigueProgressionData.points.isNotEmpty()) {
            item(key = "fatigue_progression", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f),
                    title = "Fatigue Progression",
                    content = {
                        AreaChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.fatigueProgressionData
                        )
                    }
                )
            }
        }

        if (state.movementPatternsData.slices.isNotEmpty()) {
            item(key = "movement_patterns", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    title = "Movement Patterns",
                    content = {
                        PieChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.movementPatternsData
                        )
                    }
                )
            }
        }

        if (state.executionQualityData.items.isNotEmpty()) {
            item(key = "execution_quality", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Execution Quality",
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth(),
                            data = state.executionQualityData
                        )
                    }
                )
            }
        }

        if (state.workoutDensityData.points.isNotEmpty()) {
            item(key = "workout_density", span = { GridItemSpan(4) }) {
                ChartCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f),
                    title = "Workout Density",
                    content = {
                        Sparkline(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.workoutDensityData
                        )
                    }
                )
            }
        }
    }
}

