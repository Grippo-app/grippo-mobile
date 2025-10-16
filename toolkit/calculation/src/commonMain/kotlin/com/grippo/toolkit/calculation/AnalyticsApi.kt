package com.grippo.toolkit.calculation

import com.grippo.core.state.datetime.PeriodState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.TrainingMetrics
import com.grippo.core.state.trainings.TrainingState
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.toolkit.calculation.internal.distribution.DistributionCalculator
import com.grippo.toolkit.calculation.internal.muscle.MuscleImageBuilder
import com.grippo.toolkit.calculation.internal.muscle.MuscleLoadCalculator
import com.grippo.toolkit.calculation.internal.muscle.TemporalHeatmapCalculator
import com.grippo.toolkit.calculation.internal.strength.Estimated1RMAnalytics
import com.grippo.toolkit.calculation.internal.training.MetricsAggregator
import com.grippo.toolkit.calculation.internal.training.VolumeAnalytics
import com.grippo.toolkit.calculation.models.DistributionBreakdown
import com.grippo.toolkit.calculation.models.DistributionWeighting
import com.grippo.toolkit.calculation.models.Metric
import com.grippo.toolkit.calculation.models.MetricSeries
import com.grippo.toolkit.calculation.models.MuscleLoadMatrix
import com.grippo.toolkit.calculation.models.MuscleLoadSummary

/**
 * Facade that exposes the most common workout analytics in a single place.
 * Every method intentionally mirrors how feature modules consume analytics today,
 * so callers do not need to know about the individual calculators underneath.
 */
public class AnalyticsApi(
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
) {
    private val distributionCalculator = DistributionCalculator(stringProvider, colorProvider)
    private val volumeAnalytics = VolumeAnalytics(colorProvider, stringProvider)
    private val muscleLoadCalculator = MuscleLoadCalculator(stringProvider, colorProvider)
    private val heatmapCalculator = TemporalHeatmapCalculator(stringProvider)
    private val estimated1RMAnalytics = Estimated1RMAnalytics(colorProvider, stringProvider)
    private val metricsAggregator = MetricsAggregator()
    private val muscleImageBuilder = MuscleImageBuilder(colorProvider)

    /**
     * Builds a volume series for a plain list of exercises.
     * Each point represents the session tonnage contributed by a single exercise.
     */
    public suspend fun volumeFromExercises(
        exercises: List<ExerciseState>
    ): MetricSeries = volumeAnalytics
        .computeVolumeSeriesFromExercises(exercises)

    /**
     * Builds a volume series for trainings filtered by [period].
     * The underlying calculator automatically selects the optimal bucket scale.
     */
    public suspend fun volumeFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
    ): MetricSeries = volumeAnalytics
        .computeVolumeSeriesFromTrainings(trainings, period)

    /**
     * Aggregates muscle load for the provided [exercises] and returns the group breakdown together with
     * pre-built front/back muscle images colored by intensity.
     */
    public suspend fun muscleLoadFromExercises(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): MuscleLoadSummary {
        val breakdowns = muscleLoadCalculator
            .computeMuscleLoadBreakdownsFromExercises(exercises, examples, groups)
        val images = muscleImageBuilder.generateImagesFromBreakdown(breakdowns.perMuscle)

        return MuscleLoadSummary(
            perGroup = breakdowns.perGroup,
            images = images,
        )
    }

    /**
     * Aggregates muscle load across [trainings] filtered by [period].
     * The result contains the group breakdown and ready-to-render images colored by intensity.
     */
    public suspend fun muscleLoadFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): MuscleLoadSummary {
        val breakdowns = muscleLoadCalculator
            .computeMuscleLoadBreakdownsFromTrainings(trainings, period, examples, groups)
        val images = muscleImageBuilder.generateImagesFromBreakdown(breakdowns.perMuscle)

        return MuscleLoadSummary(
            perGroup = breakdowns.perGroup,
            images = images,
        )
    }

    /**
     * Maps a single exercise example into the muscle load representation using bundle percentages.
     * Handy for dialogs that visualise how an exercise targets the body without loading a training list.
     */
    public suspend fun muscleLoadFromExample(
        example: ExerciseExampleState
    ): MuscleLoadSummary {
        val breakdowns = muscleLoadCalculator
            .computeMuscleLoadBreakdownsFromExample(example)
        val images = muscleImageBuilder.generateImagesFromBreakdown(breakdowns.perMuscle)

        return MuscleLoadSummary(
            perGroup = breakdowns.perMuscle,
            images = images,
        )
    }

    /**
     * Builds the temporal heatmap matrix for [trainings] using the provided filters.
     *
     * @param metric Bucket metric used for the heatmap normalization. Defaults to reps.
     */
    public suspend fun heatmapFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        metric: Metric = Metric.REPS,
    ): MuscleLoadMatrix = heatmapCalculator
        .computeMuscleGroupHeatmap(trainings, period, examples, groups, metric)

    /**
     * Produces an estimated 1RM series for the supplied [exercises].
     * Entries are already colourised and ready for chart rendering.
     */
    public suspend fun estimated1RmFromExercises(
        exercises: List<ExerciseState>
    ): MetricSeries = estimated1RMAnalytics
        .computeEstimated1RmFromExercises(exercises)

    /**
     * Produces an estimated 1RM series for [trainings] aggregated by the detected bucket scale.
     */
    public suspend fun estimated1RmFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
    ): MetricSeries = estimated1RMAnalytics
        .computeEstimated1RmFromTrainings(trainings, period)

    /**
     * Returns a distribution of exercise categories for the provided [exercises].
     */
    public suspend fun categoryDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown = distributionCalculator
        .calculateCategoryDistributionFromExercises(exercises, weighting)

    /**
     * Returns a distribution of exercise categories drawn from [trainings] limited by [period].
     */
    public suspend fun categoryDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown = distributionCalculator
        .calculateCategoryDistributionFromTrainings(trainings, period, weighting)

    /**
     * Returns a distribution of weight types for the provided [exercises].
     */
    public suspend fun weightTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown = distributionCalculator
        .calculateWeightTypeDistributionFromExercises(exercises, weighting)

    /**
     * Returns a distribution of weight types collected from [trainings] within [period].
     */
    public suspend fun weightTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown = distributionCalculator
        .calculateWeightTypeDistributionFromTrainings(trainings, period, weighting)

    /**
     * Returns a distribution of force types for the provided [exercises].
     */
    public suspend fun forceTypeDistributionFromExercises(
        exercises: List<ExerciseState>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown = distributionCalculator
        .calculateForceTypeDistributionFromExercises(exercises, weighting)

    /**
     * Returns a distribution of force types gathered from [trainings] within [period].
     */
    public suspend fun forceTypeDistributionFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): DistributionBreakdown = distributionCalculator
        .calculateForceTypeDistributionFromTrainings(trainings, period, weighting)

    /**
     * Aggregates training metrics from a list of iterations.
     */
    public fun metricsFromIterations(
        iterations: List<IterationState>
    ): TrainingMetrics = metricsAggregator
        .calculateIterations(iterations)

    /**
     * Aggregates training metrics from a list of exercises.
     */
    public fun metricsFromExercises(
        exercises: List<ExerciseState>
    ): TrainingMetrics = metricsAggregator
        .calculateExercises(exercises)

    /**
     * Aggregates training metrics from a list of trainings.
     */
    public fun metricsFromTrainings(
        trainings: List<TrainingState>
    ): TrainingMetrics = metricsAggregator
        .calculateTrainings(trainings)

    /**
     * Builds a muscle color preset for a specific [group] with [selectedIds] highlighted.
     */
    public suspend fun musclePresetFromSelection(
        group: MuscleGroupState<MuscleRepresentationState.Plain>,
        selectedIds: Collection<String>,
    ): MuscleColorPreset = muscleImageBuilder
        .presetFromSelection(group, selectedIds.toSet())
}
