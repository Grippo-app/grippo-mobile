package com.grippo.home.statistics

import com.grippo.calculation.AnalyticsApi
import com.grippo.calculation.models.DistributionBreakdown
import com.grippo.calculation.models.DistributionSlice
import com.grippo.calculation.models.MetricPoint
import com.grippo.calculation.models.MetricSeries
import com.grippo.calculation.models.MuscleLoadBreakdown
import com.grippo.calculation.models.MuscleLoadEntry
import com.grippo.calculation.models.MuscleLoadMatrix
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.datetime.PeriodState
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

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map {
                it.trainings
                    .flatMap { f -> f.exercises.map { m -> m.exerciseExample.id } }
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
            PeriodState.ThisYear,
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
                    totalVolume = null,
                    totalRepetitions = null,
                    averageIntensity = null,
                    exerciseVolume = null,
                    categoryDistribution = null,
                    forceTypeDistribution = null,
                    weightTypeDistribution = null,
                    muscleLoad = null,
                    temporalHeatmap = null,
                    muscleLoadSummary = null,
                )
            }
            return
        }

        val totalMetrics = analytics.metricsFromTrainings(
            trainings = trainings
        )

        val categoryDistributionChart = analytics.categoryDistributionFromTrainings(
            trainings = trainings,
            period = period,
        ).asChart()

        val weightTypeDistributionChart = analytics.weightTypeDistributionFromTrainings(
            trainings = trainings,
            period = period,
        ).asChart()

        val forceTypeDistributionChart = analytics.forceTypeDistributionFromTrainings(
            trainings = trainings,
            period = period,
        ).asChart()

        val exerciseVolumeChart = analytics.volumeFromTrainings(
            trainings = trainings,
            period = period,
        ).asChart()

        val muscleLoad = analytics.muscleLoadFromTrainings(
            trainings = trainings,
            period = period,
            examples = examples,
            groups = muscles,
        )

        val muscleLoadMatrix = analytics.heatmapFromTrainings(
            trainings = trainings,
            period = period,
            examples = examples,
            groups = muscles,
        )

        val categoryData = categoryDistributionChart.takeIf { it.slices.isNotEmpty() }
        val weightTypeData = weightTypeDistributionChart.takeIf { it.slices.isNotEmpty() }
        val forceTypeData = forceTypeDistributionChart.takeIf { it.slices.isNotEmpty() }
        val exerciseVolumeData = exerciseVolumeChart.takeIf { it.items.isNotEmpty() }
        val muscleProgressChart = muscleLoad.perGroup.asChart()
        val muscleProgressData = muscleProgressChart.takeIf { it.items.isNotEmpty() }
        val muscleSummary = muscleProgressData?.let { muscleLoad }
        val heatmapData = muscleLoadMatrix.asChart().takeIf { it.values01.isNotEmpty() }

        update {
            it.copy(
                totalVolume = totalMetrics.volume,
                totalRepetitions = totalMetrics.repetitions,
                averageIntensity = totalMetrics.intensity,
                exerciseVolume = exerciseVolumeData,
                categoryDistribution = categoryData,
                weightTypeDistribution = weightTypeData,
                forceTypeDistribution = forceTypeData,
                muscleLoad = muscleProgressData,
                muscleLoadSummary = muscleSummary,
                temporalHeatmap = heatmapData,
            )
        }
    }

    private fun DistributionBreakdown.asChart(): DSPieData = DSPieData(
        slices = slices.map { it.asChart() }
    )

    private fun DistributionSlice.asChart(): DSPieSlice = DSPieSlice(
        id = id,
        label = label,
        value = value,
        color = color,
    )

    private fun MetricSeries.asChart(): DSBarData = DSBarData(
        items = points.map { it.asChart() }
    )

    private fun MetricPoint.asChart(): DSBarItem = DSBarItem(
        label = label,
        value = value,
        color = color,
    )

    private fun MuscleLoadBreakdown.asChart(): DSProgressData = DSProgressData(
        items = entries.map { it.asChart() }
    )

    private fun MuscleLoadEntry.asChart(): DSProgressItem = DSProgressItem(
        label = label,
        value = value,
        color = color,
    )

    private fun MuscleLoadMatrix.asChart(): DSHeatmapData = DSHeatmapData(
        rows = rows,
        cols = cols,
        values01 = values01,
        rowLabels = rowLabels,
        colLabels = colLabels,
    )
}
