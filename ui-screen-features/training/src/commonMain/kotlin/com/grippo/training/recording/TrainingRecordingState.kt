package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSRadarData
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,
    val exercises: ImmutableList<ExerciseState> = persistentListOf(),
    val startAt: LocalDateTime = DateTimeUtils.now(),

    // === Basic metrics ===
    val totalVolume: VolumeFormatState = VolumeFormatState.of(0f),
    val totalRepetitions: RepetitionsFormatState = RepetitionsFormatState.of(0),
    val averageIntensity: IntensityFormatState = IntensityFormatState.of(0f),
    val exerciseVolumeData: DSBarData = DSBarData(items = emptyList()),
    val categoryDistributionData: DSPieData = DSPieData(slices = emptyList()),

    // === Muscle analysis ===
    val muscleLoadData: DSProgressData = DSProgressData(items = emptyList()),
    val muscleGroupBalanceData: DSRadarData = DSRadarData(axes = emptyList(), series = emptyList()),

    // === Performance ===
    val workoutEfficiencyData: DSProgressData = DSProgressData(items = emptyList()),
    val timeUnderTensionData: DSProgressData = DSProgressData(items = emptyList()),
    val energyExpenditureData: DSProgressData = DSProgressData(items = emptyList()),

    // === Progression and trends ===
    val intraWorkoutProgressionData: DSAreaData = DSAreaData(points = emptyList()),
    val loadOverTimeData: DSAreaData = DSAreaData(points = emptyList()),
    val fatigueProgressionData: DSAreaData = DSAreaData(points = emptyList()),

    // === Training balance ===
    val pushPullBalanceData: DSPieData = DSPieData(slices = emptyList()),
    val repRangeDistributionData: DSPieData = DSPieData(slices = emptyList()),
    val movementPatternsData: DSPieData = DSPieData(slices = emptyList()),

    // === Quality execution ===
    val executionQualityData: DSProgressData = DSProgressData(items = emptyList()),
    val techniqueQualityData: DSSparklineData = DSSparklineData(points = emptyList()),
    val weakPointsData: DSProgressData = DSProgressData(items = emptyList()),

    // === Intensity ===
    val intensityDistributionData: DSBarData = DSBarData(items = emptyList()),
    val rpeAnalysisData: DSBarData = DSBarData(items = emptyList()),
    val estimated1RMData: DSBarData = DSBarData(items = emptyList()),
    val workoutDensityData: DSSparklineData = DSSparklineData(points = emptyList()),

    // === Additinal ===
    val exerciseTypeDistributionData: DSBarData = DSBarData(items = emptyList())
)

@Immutable
internal enum class RecordingTab {
    Exercises,
    Stats
}
