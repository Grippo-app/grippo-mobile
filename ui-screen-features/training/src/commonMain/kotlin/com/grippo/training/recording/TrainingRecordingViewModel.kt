package com.grippo.training.recording

import com.grippo.calculation.distribution.DistributionCalculator
import com.grippo.calculation.models.DistributionBreakdown
import com.grippo.calculation.models.DistributionSlice
import com.grippo.calculation.models.MetricPoint
import com.grippo.calculation.models.MetricSeries
import com.grippo.calculation.models.MuscleLoadBreakdown
import com.grippo.calculation.models.MuscleLoadEntry
import com.grippo.calculation.muscle.MuscleLoadCalculator
import com.grippo.calculation.training.MetricsAggregator
import com.grippo.calculation.training.VolumeAnalytics
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.training.toDomain
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.PercentageFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class)
internal class TrainingRecordingViewModel(
    muscleFeature: MuscleFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    colorProvider: ColorProvider,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState()
), TrainingRecordingContract {

    private val metricsAggregator = MetricsAggregator()
    private val volumeAnalytics = VolumeAnalytics(colorProvider, stringProvider)
    private val distributionCalculator = DistributionCalculator(stringProvider, colorProvider)
    private val muscleLoadCalculator = MuscleLoadCalculator(stringProvider, colorProvider)

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map {
                it.exercises.mapNotNull { exercise -> exercise.exerciseExample.id }.toSet().toList()
            }
            .distinctUntilChanged()
            .flatMapLatest { ids -> exerciseExampleFeature.observeExerciseExamples(ids) }
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        state
            .map { Triple(it.exercises, it.examples, it.muscles) }
            .debounce(200)
            .distinctUntilChanged()
            .onEach { generateStatistics() }
            .safeLaunch()

        safeLaunch {
            val training = trainingFeature.getDraftTraining().firstOrNull()
            provideDraftTraining(training)
        }
    }

    private fun provideDraftTraining(value: SetTraining?) {
        val exercises = value?.exercises?.toState() ?: return
        val startAt = DateTimeUtils.minus(DateTimeUtils.now(), value.duration)
        update { it.copy(exercises = exercises.toPersistentList(), startAt = startAt) }
    }

    private fun provideMuscles(value: List<MuscleGroup>) {
        val muscles = value.toState()
        update { it.copy(muscles = muscles) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val examples = value.toState()
        update { it.copy(examples = examples) }
    }

    override fun onAddExercise() {
        val lastExampleId = state.value.exercises
            .lastOrNull()
            ?.exerciseExample
            ?.id

        val lastTargetMuscleId = state.value.examples
            .firstOrNull { it.value.id == lastExampleId }
            ?.bundles
            ?.maxByOrNull {
                (it.percentage as? PercentageFormatState.Valid)?.value ?: Int.MIN_VALUE
            }
            ?.muscle
            ?.id

        val lastTargetMuscleGroupId = state.value.muscles
            .find { it.muscles.any { a -> a.value.id == lastTargetMuscleId } }
            ?.id

        val dialog = DialogConfig.ExerciseExamplePicker(
            targetMuscleGroupId = lastTargetMuscleGroupId,
            onResult = { example ->
                val exercise = ExerciseState(
                    id = Uuid.random().toString(),
                    name = example.value.name,
                    iterations = persistentListOf(),
                    exerciseExample = example.value,
                    metrics = TrainingMetrics(
                        volume = VolumeFormatState.of(0f),
                        repetitions = RepetitionsFormatState.of(0),
                        intensity = IntensityFormatState.of(0f),
                    ),
                )

                navigateTo(TrainingRecordingDirection.ToExercise(exercise))
            }
        )

        dialogController.show(dialog)
    }

    override fun onEditExercise(id: String) {
        val exercise = state.value.exercises.find { it.id == id } ?: return
        navigateTo(TrainingRecordingDirection.ToExercise(exercise))
    }

    fun updateExercise(value: ExerciseState) {
        update {
            val exercises = it.exercises

            val index = exercises.indexOfFirst { exercise -> exercise.id == value.id }

            val updatedExercises = if (index >= 0) {
                exercises.toMutableList().apply { this[index] = value }
            } else {
                exercises + value
            }

            it.copy(exercises = updatedExercises.toPersistentList())
        }

        saveDraftTraining()
    }

    override fun onSelectTab(tab: RecordingTab) {
        update { it.copy(tab = tab) }
    }

    override fun onDeleteExercise(id: String) {
        update { s ->
            val exercises = s.exercises
                .toMutableList()
                .apply {
                    val idx = indexOfFirst { it.id == id }
                    if (idx >= 0) removeAt(idx)
                }.toPersistentList()

            s.copy(exercises = exercises)
        }

        safeLaunch {
            trainingFeature.getDraftTraining().firstOrNull()
            if (state.value.exercises.isEmpty()) {
                trainingFeature.deleteDraftTraining().getOrThrow()
            } else {
                saveDraftTraining()
            }
        }
    }

    override fun onSave() {
        val direction = TrainingRecordingDirection.ToCompleted(
            exercises = state.value.exercises,
            startAt = state.value.startAt
        )

        navigateTo(direction)
    }

    override fun onBack() {
        if (state.value.exercises.isEmpty()) {
            safeLaunch {
                trainingFeature.deleteDraftTraining()
                navigateTo(TrainingRecordingDirection.Back)
            }
        } else {
            safeLaunch {
                val dialog = DialogConfig.Confirmation(
                    title = stringProvider.get(Res.string.training_progress_lost_title),
                    description = stringProvider.get(Res.string.training_progress_lost_description),
                    onResult = {
                        safeLaunch {
                            trainingFeature.deleteDraftTraining()
                            navigateTo(TrainingRecordingDirection.Back)
                        }
                    }
                )

                dialogController.show(dialog)
            }
        }
    }

    private fun saveDraftTraining() {
        safeLaunch {
            val exercises = state.value.exercises
            val duration = DateTimeUtils.ago(state.value.startAt)

            val totals = metricsAggregator.calculateExercises(exercises)

            val training = SetTraining(
                exercises = exercises.toDomain(),
                duration = duration,
                volume = totals.volume.value ?: 0f,
                intensity = totals.intensity.value ?: 0f,
                repetitions = totals.repetitions.value ?: 0
            )

            trainingFeature.setDraftTraining(training).getOrThrow()
        }
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
                    weightTypeDistributionData = DSPieData(slices = emptyList()),
                    muscleLoadData = DSProgressData(items = emptyList()),
                    muscleLoadMuscles = MuscleLoadBreakdown(entries = emptyList()),
                )
            }
            return
        }

        val totalMetrics = metricsAggregator
            .calculateExercises(exercises = exercises)

        val categoryDistributionData = distributionCalculator
            .calculateCategoryDistributionFromExercises(exercises = exercises)
            .asChart()

        val weightTypeDistributionData = distributionCalculator
            .calculateWeightTypeDistributionFromExercises(exercises = exercises)
            .asChart()

        val forceTypeDistributionData = distributionCalculator
            .calculateForceTypeDistributionFromExercises(exercises = exercises)
            .asChart()

        val exerciseVolumeSeries = volumeAnalytics
            .calculateExerciseVolumeChartFromExercises(exercises = exercises)

        val muscleLoadVisualization = muscleLoadCalculator
            .calculateMuscleLoadVisualizationFromExercises(
                exercises = exercises,
                examples = examples,
                groups = muscles,
            )

        update {
            it.copy(
                totalVolume = totalMetrics.volume,
                totalRepetitions = totalMetrics.repetitions,
                averageIntensity = totalMetrics.intensity,
                exerciseVolumeData = exerciseVolumeSeries.asChart(),
                categoryDistributionData = categoryDistributionData,
                weightTypeDistributionData = weightTypeDistributionData,
                forceTypeDistributionData = forceTypeDistributionData,
                muscleLoadData = muscleLoadVisualization.perGroup.asChart(),
                muscleLoadMuscles = muscleLoadVisualization.perMuscle,
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
}
