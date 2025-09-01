package com.grippo.training.recording

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics
import com.grippo.training.recording.calculation.ExampleAnalyticsCalculator
import com.grippo.training.recording.calculation.ExerciseAnalyticsCalculator
import com.grippo.training.recording.calculation.MuscleAnalyticsCalculator
import com.grippo.training.recording.calculation.QualityAnalyticsCalculator
import com.grippo.training.recording.calculation.TrainingMetricsCalculator
import com.grippo.training.recording.calculation.WorkoutEfficiencyCalculator

internal class TrainingStatisticsFacade(
    stringProvider: StringProvider,
    colorProvider: ColorProvider
) {
    private val metrics: TrainingMetricsCalculator =
        TrainingMetricsCalculator(stringProvider)
    private val exercises: ExerciseAnalyticsCalculator =
        ExerciseAnalyticsCalculator(stringProvider, colorProvider)
    private val examples: ExampleAnalyticsCalculator =
        ExampleAnalyticsCalculator(stringProvider, colorProvider)
    private val muscles: MuscleAnalyticsCalculator =
        MuscleAnalyticsCalculator(stringProvider, colorProvider)
    private val efficiency: WorkoutEfficiencyCalculator =
        WorkoutEfficiencyCalculator(stringProvider, colorProvider)
    private val quality: QualityAnalyticsCalculator =
        QualityAnalyticsCalculator(stringProvider, colorProvider)

    // === Base Metrics ===
    suspend fun calculateTotalMetrics(
        exercises: List<ExerciseState>
    ): TrainingMetrics =
        metrics.calculateTotalMetrics(exercises)

    // === Exercise Analytics ===
    suspend fun calculateExerciseVolumeChart(
        exercises: List<ExerciseState>,
    ): DSBarData =
        this.exercises.calculateExerciseVolumeChart(exercises)

    suspend fun calculateIntensityDistribution(
        exercises: List<ExerciseState>,
    ): DSBarData =
        this.exercises.calculateIntensityDistribution(exercises)

    suspend fun calculateIntraWorkoutProgression(
        exercises: List<ExerciseState>,
    ): DSAreaData =
        this.exercises.calculateIntraWorkoutProgression(exercises)

    suspend fun calculateWeakPoints(
        exercises: List<ExerciseState>,
    ): DSProgressData =
        this.exercises.calculateWeakPoints(exercises)

    suspend fun calculateEstimated1RM(exercises: List<ExerciseState>): DSBarData =
        this.exercises.calculateEstimated1RM(exercises)

    // === Category Analytics ===
    suspend fun calculateCategoryDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData =
        examples.calculateCategoryDistribution(exercises)

    suspend fun calculateWeightTypeDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData =
        examples.calculateWeightTypeDistribution(exercises)

    suspend fun calculateForceTypeDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData =
        examples.calculateForceTypeDistribution(exercises)

    suspend fun calculateExperienceDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData =
        examples.calculateExperienceDistribution(exercises)

    // === Muscle Analytics ===
    suspend fun calculateMuscleLoadDistribution(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>
    ): DSProgressData =
        muscles.calculateMuscleLoadDistribution(exercises, examples)

    suspend fun calculatePushPullBalance(
        exercises: List<ExerciseState>,
    ): DSPieData =
        muscles.calculatePushPullBalance(exercises)

    // === Efficiency Analytics ===
    suspend fun calculateWorkoutEfficiency(
        exercises: List<ExerciseState>,
        workoutDurationMinutes: Int,
    ): DSProgressData =
        efficiency.calculateWorkoutEfficiency(exercises, workoutDurationMinutes)

    suspend fun calculateWorkoutDensity(
        exercises: List<ExerciseState>,
        workoutDurationMinutes: Int,
    ): DSSparklineData =
        efficiency.calculateWorkoutDensity(exercises, workoutDurationMinutes)

    suspend fun calculateLoadOverTime(
        exercises: List<ExerciseState>,
    ): DSAreaData =
        efficiency.calculateLoadOverTime(exercises)

    suspend fun calculateFatigueProgression(
        exercises: List<ExerciseState>,
    ): DSAreaData =
        efficiency.calculateFatigueProgression(exercises)

    suspend fun calculateEnergyExpenditure(
        exercises: List<ExerciseState>,
        bodyWeightKg: Float,
    ): DSProgressData =
        efficiency.calculateEnergyExpenditure(exercises, bodyWeightKg)

    // === Quality Analytics ===
    suspend fun calculateExecutionQuality(
        exercises: List<ExerciseState>
    ): DSProgressData =
        quality.calculateExecutionQuality(exercises)

    suspend fun calculateRepRangeDistribution(
        exercises: List<ExerciseState>
    ): DSPieData =
        quality.calculateRepRangeDistribution(exercises)

    suspend fun calculateTechniqueQuality(
        exercises: List<ExerciseState>
    ): DSSparklineData =
        quality.calculateTechniqueQuality(exercises)

    suspend fun calculateTimeUnderTension(
        exercises: List<ExerciseState>
    ): DSProgressData =
        quality.calculateTimeUnderTension(exercises)

    suspend fun calculateRPEAnalysis(
        exercises: List<ExerciseState>
    ): DSBarData =
        quality.calculateRPEAnalysis(exercises)

    suspend fun calculateMovementPatterns(
        exercises: List<ExerciseState>
    ): DSPieData =
        quality.calculateMovementPatterns(exercises)
}
