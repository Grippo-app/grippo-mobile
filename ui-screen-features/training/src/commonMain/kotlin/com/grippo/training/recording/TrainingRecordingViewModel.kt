package com.grippo.training.recording

import com.grippo.calculation.AnalyticsCalculator
import com.grippo.calculation.DistributionCalculator
import com.grippo.calculation.LoadCalculator
import com.grippo.calculation.MetricsAggregator
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.state.domain.training.toDomain
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val analyticsCalculator = AnalyticsCalculator(colorProvider, stringProvider)
    private val distributionCalculator = DistributionCalculator(stringProvider, colorProvider)
    private val loadCalculator = LoadCalculator(stringProvider, colorProvider)

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
            .map { Triple(it.exercises, it.examples, it.muscles) }
            .debounce(200)
            .distinctUntilChanged()
            .onEach { generateStatistics() }
            .safeLaunch()
    }

    override fun onAddExercise() {
        val dialog = DialogConfig.ExerciseExamplePicker(
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

    private fun provideMuscles(value: List<MuscleGroup>) {
        val muscles = value.toState()
        update { it.copy(muscles = muscles) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val examples = value.toState()
        update { it.copy(examples = examples) }
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
    }

    override fun onSave() {
        val direction = TrainingRecordingDirection.ToCompleted(
            exercises = state.value.exercises,
            startAt = state.value.startAt
        )

        navigateTo(direction)
    }

    override fun onBack() {
        safeLaunch {
            val dialog = DialogConfig.Confirmation(
                title = stringProvider.get(Res.string.training_progress_lost_title),
                description = stringProvider.get(Res.string.training_progress_lost_description),
                onResult = { navigateTo(TrainingRecordingDirection.Back) }
            )

            dialogController.show(dialog)
        }
    }

    private fun saveDraftTraining() {
        safeLaunch {
            val exercises = state.value.exercises
            val duration = DateTimeUtils.ago(state.value.startAt)

            val totals = metricsAggregator.calculateExercises(exercises)

            val training = SetTraining(
                exercises = exercises.toDomain(),
                duration = duration.inWholeMinutes,
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
                    exerciseVolumeData = DSBarData(items = emptyList()) to null,
                    categoryDistributionData = DSPieData(slices = emptyList()) to null,
                    forceTypeDistributionData = DSPieData(slices = emptyList()) to null,
                    experienceDistributionData = DSPieData(slices = emptyList()) to null,
                    weightTypeDistributionData = DSPieData(slices = emptyList()) to null,
                    muscleLoadData = DSProgressData(items = emptyList()) to null,
                    percent1RMData = DSAreaData(points = emptyList()) to null,
                    stimulusData = DSAreaData(points = emptyList()) to null,
                    estimated1RMData = DSBarData(items = emptyList()) to null,
                )
            }
            return
        }

        val totalMetrics = metricsAggregator.calculateExercises(
            exercises = exercises
        )
        val categoryDistributionData =
            distributionCalculator.calculateCategoryDistributionFromExercises(
                exercises = exercises
            )
        val weightTypeDistributionData =
            distributionCalculator.calculateWeightTypeDistributionFromExercises(
                exercises = exercises
            )
        val forceTypeDistributionData =
            distributionCalculator.calculateForceTypeDistributionFromExercises(
                exercises = exercises
            )
        val experienceDistributionData =
            distributionCalculator.calculateExperienceDistributionFromExercises(
                exercises = exercises
            )
        val exerciseVolumeData =
            analyticsCalculator.calculateExerciseVolumeChartFromExercises(
                exercises = exercises
            )
        val muscleLoadData = loadCalculator.calculateMuscleLoadDistributionFromExercises(
            exercises = exercises,
            examples = examples,
            groups = muscles,
        )
        val percent1RMData =
            analyticsCalculator.calculateIntraProgressionPercent1RMFromExercises(
                exercises = exercises
            )
        val stimulusData =
            analyticsCalculator.calculateIntraProgressionStimulusFromExercises(
                exercises = exercises
            )
        val estimated1RMData =
            analyticsCalculator.calculateEstimated1RMFromExercises(
                exercises = exercises
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
                percent1RMData = percent1RMData,
                stimulusData = stimulusData,
                estimated1RMData = estimated1RMData,
            )
        }
    }
}