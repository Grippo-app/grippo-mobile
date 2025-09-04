package com.grippo.home.statistics

import com.grippo.calculation.MetricsAggregator
import com.grippo.calculation.MuscleLoadCalculator
import com.grippo.calculation.MuscleLoadCalculator.RelativeMode
import com.grippo.calculation.TrainingAnalyticsCalculator
import com.grippo.calculation.TrainingDistributionCalculator
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    private val metricsAggregator: MetricsAggregator =
        MetricsAggregator()
    private val trainingAnalyticsCalculator: TrainingAnalyticsCalculator =
        TrainingAnalyticsCalculator(colorProvider)
    private val trainingExamplesCalculator: TrainingDistributionCalculator =
        TrainingDistributionCalculator(stringProvider, colorProvider)
    private val trainingMuscleCalculator: MuscleLoadCalculator =
        MuscleLoadCalculator(stringProvider, colorProvider)

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { it.exercises.mapNotNull { m -> m.exerciseExample?.id }.toSet().toList() }
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
            .map { Triple(it.exercises, it.examples, it.muscles) }
            .debounce(200)
            .distinctUntilChanged()
            .onEach { generateStatistics() }
            .safeLaunch()
    }

    private fun provideTrainings(list: List<Training>) {
        val exercises = list.toState().flatMap { it.exercises }.toPersistentList()
        update { it.copy(exercises = exercises) }
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

    override fun onFiltersClick() {
        val dialog = DialogConfig.FilterPicker(
            initial = persistentListOf(),
            onResult = { value ->
            }
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(HomeStatisticsDirection.Back)
    }

    private suspend fun generateStatistics() {
        val exercises = state.value.exercises
        val examples = state.value.examples
        val muscles = state.value.muscles

        if (exercises.isEmpty()) {
            update {
                it.copy(
                    totalVolume = VolumeFormatState.of(0f),
                    totalRepetitions = RepetitionsFormatState.of(0),
                    averageIntensity = IntensityFormatState.of(0f),
                    exerciseVolumeData = DSBarData(items = emptyList()),
                    categoryDistributionData = DSPieData(slices = emptyList()),
                    forceTypeDistributionData = DSPieData(slices = emptyList()),
                    experienceDistributionData = DSPieData(slices = emptyList()),
                    weightTypeDistributionData = DSPieData(slices = emptyList()),
                    muscleLoadData = DSProgressData(items = emptyList()),
                    intraProgressionData = DSAreaData(points = emptyList()),
                )
            }
            return
        }

        val totalMetrics = metricsAggregator.calculateExercises(
            exercises = exercises
        )
        val categoryDistributionData =
            trainingExamplesCalculator.calculateCategoryDistributionFromExercises(
                exercises = exercises
            )
        val weightTypeDistributionData =
            trainingExamplesCalculator.calculateWeightTypeDistributionFromExercises(
                exercises = exercises
            )
        val forceTypeDistributionData =
            trainingExamplesCalculator.calculateForceTypeDistributionFromExercises(
                exercises = exercises
            )
        val experienceDistributionData =
            trainingExamplesCalculator.calculateExperienceDistributionFromExercises(
                exercises = exercises
            )
        val exerciseVolumeData =
            trainingAnalyticsCalculator.calculateExerciseVolumeChartFromExercises(
                exercises = exercises
            )
        val intraProgressionData =
            trainingAnalyticsCalculator.calculateIntraProgressionPercent1RMFromExercises(
                exercises = exercises
            ).data
        val muscleLoadData = trainingMuscleCalculator.calculateMuscleLoadDistributionFromExercises(
            exercises = exercises,
            examples = examples,
            groups = muscles,
            mode = MuscleLoadCalculator.Mode.RELATIVE,
            relativeMode = RelativeMode.SUM,
            workload = MuscleLoadCalculator.Workload.Volume
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
                intraProgressionData = intraProgressionData,
            )
        }
    }
}