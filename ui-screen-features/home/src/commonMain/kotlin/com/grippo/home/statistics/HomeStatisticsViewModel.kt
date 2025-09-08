package com.grippo.home.statistics

import com.grippo.calculation.DistributionCalculator
import com.grippo.calculation.MetricsAggregator
import com.grippo.calculation.TemporalHeatmapCalculator
import com.grippo.calculation.TotalLoadCalculator
import com.grippo.calculation.VolumeAnalytics
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class)
internal class HomeStatisticsViewModel(
    muscleFeature: MuscleFeature,
    trainingFeature: TrainingFeature,
    colorProvider: ColorProvider,
    stringProvider: StringProvider,
    private val dialogController: DialogController,
    private val exerciseExampleFeature: ExerciseExampleFeature,
) : BaseViewModel<HomeStatisticsState, HomeStatisticsDirection, HomeStatisticsLoader>(
    HomeStatisticsState()
), HomeStatisticsContract {

    private val metricsAggregator = MetricsAggregator()
    private val temporalHeatmapCalculator = TemporalHeatmapCalculator(stringProvider)
    private val volumeAnalytics = VolumeAnalytics(colorProvider, stringProvider)
    private val distributionCalculator = DistributionCalculator(stringProvider, colorProvider)
    private val totalLoadCalculator = TotalLoadCalculator(stringProvider, colorProvider)

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map {
                it.trainings
                    .flatMap { f -> f.exercises.mapNotNull { m -> m.exerciseExample?.id } }
                    .toSet().toList()
            }
            .distinctUntilChanged()
            .flatMapLatest { ids -> exerciseExampleFeature.observeExerciseExamples(ids) }
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        state
            .map { it.period.range }
            .onEach { trainingFeature.getTrainings(start = it.from, end = it.to).getOrThrow() }
            .safeLaunch()

        state
            .map { it.period.range }
            .flatMapLatest { r -> trainingFeature.observeTrainings(start = r.from, end = r.to) }
            .onEach(::provideTrainings)
            .safeLaunch()

        state
            .map { s -> listOf(s.trainings, s.examples, s.muscles, s.period) }
            .debounce(200)
            .distinctUntilChanged()
            .mapLatest { generateStatistics() }
            .safeLaunch(loader = HomeStatisticsLoader.Charts)
    }

    private fun provideTrainings(list: List<Training>) {
        val exercises = list.toState()
        update { it.copy(trainings = exercises) }
    }

    private fun provideMuscles(value: List<MuscleGroup>) {
        val muscles = value.toState()
        update { it.copy(muscles = muscles) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val examples = value.toState()
        update { it.copy(examples = examples) }
    }

    override fun onSelectPeriod() {
        val custom = (state.value.period as? PeriodState.CUSTOM) ?: PeriodState.CUSTOM(
            range = DateTimeUtils.thisWeek(),
            limitations = DateTimeUtils.trailingYear()
        )

        val available = listOf(
            PeriodState.ThisDay,
            PeriodState.ThisWeek,
            PeriodState.ThisMonth,
            custom
        )

        val dialog = DialogConfig.PeriodPicker(
            initial = state.value.period,
            available = available,
            onResult = { value -> update { it.copy(period = value) } }
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(HomeStatisticsDirection.Back)
    }

    private suspend fun generateStatistics() {
        val trainings = state.value.trainings
        val examples = state.value.examples
        val muscles = state.value.muscles
        val period = state.value.period

        if (trainings.isEmpty()) {
            update {
                it.copy(
                    totalVolume = VolumeFormatState.of(0f),
                    totalRepetitions = RepetitionsFormatState.of(0),
                    averageIntensity = IntensityFormatState.of(0f),
                    exerciseVolumeData = DSBarData(items = emptyList()) to null,
                    categoryDistributionData = DSPieData(slices = emptyList()) to null,
                    forceTypeDistributionData = DSPieData(slices = emptyList()) to null,
                    experienceDistributionData = DSPieData(slices = emptyList()) to null,
                    weightTypeDistributionData = DSPieData(slices = emptyList()) to null,
                    muscleLoadData = DSProgressData(items = emptyList()) to null,
                    temporalHeatmapData = DSHeatmapData(
                        rows = 0,
                        cols = 0,
                        values01 = emptyList()
                    ) to null
                )
            }
            return
        }

        val totalMetrics =
            metricsAggregator.calculateTrainings(
                trainings = trainings
            )
        val categoryDistributionData =
            distributionCalculator.calculateCategoryDistributionFromTrainings(
                trainings = trainings,
                period = period
            )
        val weightTypeDistributionData =
            distributionCalculator.calculateWeightTypeDistributionFromTrainings(
                trainings = trainings,
                period = period
            )
        val forceTypeDistributionData =
            distributionCalculator.calculateForceTypeDistributionFromTrainings(
                trainings = trainings,
                period = period
            )
        val experienceDistributionData =
            distributionCalculator.calculateExperienceDistributionFromTrainings(
                trainings = trainings,
                period = period
            )
        val exerciseVolumeData =
            volumeAnalytics.calculateExerciseVolumeChartFromTrainings(
                trainings = trainings,
                period = period
            )
        val muscleLoadData =
            totalLoadCalculator.calculateMuscleLoadDistributionFromTrainings(
                trainings = trainings,
                examples = examples,
                groups = muscles,
                period = period
            )
        val temporalHeatmapData =
            temporalHeatmapCalculator.calculateMuscleGroupHeatmapFromTrainings(
                trainings = trainings,
                period = period,
                examples = examples,
                groups = muscles,
                metric = TemporalHeatmapCalculator.Metric.REPS
            )

        update {
            it.copy(
                totalVolume = totalMetrics.volume,
                totalRepetitions = totalMetrics.repetitions,
                averageIntensity = totalMetrics.intensity,
                exerciseVolumeData = exerciseVolumeData,
                categoryDistributionData = categoryDistributionData,
                weightTypeDistributionData = weightTypeDistributionData,
                forceTypeDistributionData = forceTypeDistributionData,
                experienceDistributionData = experienceDistributionData,
                muscleLoadData = muscleLoadData,
                temporalHeatmapData = temporalHeatmapData,
            )
        }
    }
}