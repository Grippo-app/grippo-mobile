package com.grippo.training.recording.pages

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
import androidx.compose.ui.Modifier
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
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_volume
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

        item(key = "summary_chips_1", span = { GridItemSpan(4) }) {
            Row(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth(),
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
                        .animateItem()
                        .fillMaxWidth()
                        .aspectRatio(1.8f),
                    title = AppTokens.strings.res(Res.string.exercise_volume),
                    content = {
                        BarChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.exerciseVolumeData
                        )
                    }
                )
            }
        }

        if (state.categoryDistributionData.slices.isNotEmpty()) {
            item(key = "category_distribution", span = { GridItemSpan(1) }) {
                ChartCard(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    title = "Category",
                    content = {
                        PieChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.categoryDistributionData
                        )
                    }
                )
            }
        }

        if (state.weightTypeDistributionData.slices.isNotEmpty()) {
            item(key = "weight_type_distribution", span = { GridItemSpan(1) }) {
                ChartCard(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    title = "Weight type",
                    content = {
                        PieChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.weightTypeDistributionData
                        )
                    }
                )
            }
        }

        if (state.experienceDistributionData.slices.isNotEmpty()) {
            item(key = "experience_distribution", span = { GridItemSpan(1) }) {
                ChartCard(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    title = "Experience",
                    content = {
                        PieChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.experienceDistributionData
                        )
                    }
                )
            }
        }

        if (state.forceTypeDistributionData.slices.isNotEmpty()) {
            item(key = "force_type_distribution", span = { GridItemSpan(1) }) {
                ChartCard(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    title = "Force type",
                    content = {
                        PieChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.forceTypeDistributionData
                        )
                    }
                )
            }
        }

        if (state.muscleLoadData.items.isNotEmpty()) {
            item(key = "muscle_load") {
                ChartCard(
                    modifier = Modifier
                        .animateItem()
                        .padding(end = AppTokens.dp.screen.horizontalPadding)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    title = "Muscle Load Distribution",
                    content = {
                        ProgressChart(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            data = state.muscleLoadData
                        )
                    }
                )
            }
        }

//        if (state.muscleGroupBalanceData.axes.isNotEmpty()) {
//            item(key = "muscle_balance") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(start = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Muscle Group Balance",
//                    content = {
//                        RadarChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.muscleGroupBalanceData
//                        )
//                    }
//                )
//            }
//        }
//
//        if (state.workoutEfficiencyData.items.isNotEmpty()) {
//            item(key = "workout_efficiency") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(end = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Workout Efficiency",
//                    content = {
//                        ProgressChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.workoutEfficiencyData
//                        )
//                    }
//                )
//            }
//        }
//
//        // RPE Analysis
//        if (state.rpeAnalysisData.items.isNotEmpty()) {
//            item(key = "rpe_analysis", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1.4f),
//                    title = "RPE Analysis (Perceived Exertion)",
//                    content = {
//                        BarChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.rpeAnalysisData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Push/Pull Balance
//        if (state.pushPullBalanceData.slices.isNotEmpty()) {
//            item(key = "push_pull_balance") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(start = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Push/Pull Balance",
//                    content = {
//                        PieChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.pushPullBalanceData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Rep Range Distribution
//        if (state.repRangeDistributionData.slices.isNotEmpty()) {
//            item(key = "rep_range_distribution") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(end = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Rep Range Focus",
//                    content = {
//                        PieChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.repRangeDistributionData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Intra-Workout Progression
//        if (state.intraWorkoutProgressionData.points.isNotEmpty()) {
//            item(key = "intra_workout_progression", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1.6f),
//                    title = "Performance Throughout Workout",
//                    content = {
//                        AreaChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.intraWorkoutProgressionData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Estimated 1RM
//        if (state.estimated1RMData.items.isNotEmpty()) {
//            item(key = "estimated_1rm", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1.4f),
//                    title = "Estimated 1RM",
//                    content = {
//                        BarChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.estimated1RMData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Technique Quality Sparkline
//        if (state.techniqueQualityData.points.isNotEmpty()) {
//            item(key = "technique_quality", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(2f),
//                    title = "Technique Consistency",
//                    content = {
//                        Sparkline(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.techniqueQualityData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Weak Points Analysis
//        if (state.weakPointsData.items.isNotEmpty()) {
//            item(key = "weak_points") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(start = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Potential Weak Points",
//                    content = {
//                        ProgressChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.weakPointsData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Time Under Tension
//        if (state.timeUnderTensionData.items.isNotEmpty()) {
//            item(key = "time_under_tension") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(end = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Time Under Tension",
//                    content = {
//                        ProgressChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.timeUnderTensionData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Energy Expenditure
//        if (state.energyExpenditureData.items.isNotEmpty()) {
//            item(key = "energy_expenditure") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(start = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Energy Expenditure",
//                    content = {
//                        ProgressChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.energyExpenditureData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Load Over Time
//        if (state.loadOverTimeData.points.isNotEmpty()) {
//            item(key = "load_over_time", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1.6f),
//                    title = "Load Over Time",
//                    content = {
//                        AreaChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.loadOverTimeData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Fatigue Progression
//        if (state.fatigueProgressionData.points.isNotEmpty()) {
//            item(key = "fatigue_progression", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1.6f),
//                    title = "Fatigue Progression",
//                    content = {
//                        AreaChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.fatigueProgressionData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Movement Patterns
//        if (state.movementPatternsData.slices.isNotEmpty()) {
//            item(key = "movement_patterns") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(end = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Movement Patterns",
//                    content = {
//                        PieChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.movementPatternsData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Execution Quality
//        if (state.executionQualityData.items.isNotEmpty()) {
//            item(key = "execution_quality") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(start = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Execution Quality",
//                    content = {
//                        ProgressChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.executionQualityData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Intensity Distribution
//        if (state.intensityDistributionData.items.isNotEmpty()) {
//            item(key = "intensity_distribution", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1.8f),
//                    title = "Intensity Distribution",
//                    content = {
//                        BarChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.intensityDistributionData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Workout Density Sparkline
//        if (state.workoutDensityData.points.isNotEmpty()) {
//            item(key = "workout_density", span = StaggeredGridItemSpan.FullLine) {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(2f),
//                    title = "Workout Density",
//                    content = {
//                        Sparkline(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.workoutDensityData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Exercise Type Distribution
//        if (state.exerciseTypeDistributionData.items.isNotEmpty()) {
//            item(key = "exercise_type_distribution") {
//                ChartCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(end = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth()
//                        .aspectRatio(1f),
//                    title = "Exercise Types",
//                    content = {
//                        BarChart(
//                            modifier = Modifier.fillMaxWidth().weight(1f),
//                            data = state.exerciseTypeDistributionData
//                        )
//                    }
//                )
//            }
//        }
//
//        // Empty state when no exercises
//        if (state.exercises.isEmpty()) {
//            item(key = "empty_state", span = StaggeredGridItemSpan.FullLine) {
//                Text(
//                    modifier = Modifier
//                        .animateItem()
//                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
//                        .fillMaxWidth(),
//                    text = "Add exercises to see statistics",
//                    style = AppTokens.typography.b14Med(),
//                    color = AppTokens.colors.text.secondary
//                )
//            }
//        }
    }
}

