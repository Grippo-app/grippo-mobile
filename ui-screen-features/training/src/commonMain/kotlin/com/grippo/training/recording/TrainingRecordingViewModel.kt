package com.grippo.training.recording

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.uuid.Uuid

internal class TrainingRecordingViewModel(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val muscleFeature: MuscleFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState()
), TrainingRecordingContract {

    private val statisticsCalculator = TrainingStatisticsFacade(
        stringProvider = stringProvider,
        colorProvider = colorProvider
    )

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

        safeLaunch { generateStatistics() }
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

        safeLaunch { generateStatistics() }
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

    private suspend fun generateStatistics() {
        colorProvider.get()

        val exercises = state.value.exercises
        val examples = state.value.examples
        val muscles = state.value.muscles

        if (exercises.isEmpty()) {
            // Reset statistics to empty state
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
                    workoutEfficiencyData = DSProgressData(items = emptyList()),
                    timeUnderTensionData = DSProgressData(items = emptyList()),
                    energyExpenditureData = DSProgressData(items = emptyList()),
                    intraWorkoutProgressionData = DSAreaData(points = emptyList()),
                    loadOverTimeData = DSAreaData(points = emptyList()),
                    fatigueProgressionData = DSAreaData(points = emptyList()),
                    repRangeDistributionData = DSPieData(slices = emptyList()),
                    movementPatternsData = DSPieData(slices = emptyList()),
                    executionQualityData = DSProgressData(items = emptyList()),
                    techniqueQualityData = DSSparklineData(points = emptyList()),
                    intensityDistributionData = DSBarData(items = emptyList()),
                    rpeAnalysisData = DSBarData(items = emptyList()),
                    estimated1RMData = DSBarData(items = emptyList()),
                    workoutDensityData = DSSparklineData(points = emptyList()),
                )
            }
            return
        }

        val workoutDurationMinutes = exercises.size * 15

        // Total
        val totalMetrics = statisticsCalculator
            .calculateTotalMetrics(exercises)

        // ExampleAnalytics
        val categoryDistributionData = statisticsCalculator
            .calculateCategoryDistribution(exercises)
        val weightTypeDistributionData = statisticsCalculator
            .calculateWeightTypeDistribution(exercises)
        val forceTypeDistributionData = statisticsCalculator
            .calculateForceTypeDistribution(exercises)
        val experienceDistributionData = statisticsCalculator
            .calculateExperienceDistribution(exercises)

        // ExerciseAnalytics
        val exerciseVolumeData = statisticsCalculator
            .calculateExerciseVolumeChart(exercises)
        val intensityDistributionData = statisticsCalculator
            .calculateIntensityDistribution(exercises)
        val intraWorkoutProgressionData = statisticsCalculator
            .calculateIntraWorkoutProgression(exercises)
        val estimated1RMData = statisticsCalculator
            .calculateEstimated1RM(exercises)

        // MuscleAnalytics
        val muscleLoadData = statisticsCalculator
            .calculateMuscleLoadDistribution(exercises, examples, muscles)

        val workoutEfficiencyData = statisticsCalculator
            .calculateWorkoutEfficiency(exercises, workoutDurationMinutes)

        val timeUnderTensionData = statisticsCalculator
            .calculateTimeUnderTension(exercises)

        val energyExpenditureData = statisticsCalculator // 75kg default weight
            .calculateEnergyExpenditure(exercises, 75f)

        val loadOverTimeData = statisticsCalculator
            .calculateLoadOverTime(exercises)

        val fatigueProgressionData = statisticsCalculator
            .calculateFatigueProgression(exercises)

        val repRangeDistributionData = statisticsCalculator
            .calculateRepRangeDistribution(exercises)

        val movementPatternsData = statisticsCalculator
            .calculateMovementPatterns(exercises)

        val executionQualityData = statisticsCalculator
            .calculateExecutionQuality(exercises)

        val techniqueQualityData = statisticsCalculator
            .calculateTechniqueQuality(exercises)

        val rpeAnalysisData = statisticsCalculator
            .calculateRPEAnalysis(exercises)

        val workoutDensityData = statisticsCalculator
            .calculateWorkoutDensity(exercises, workoutDurationMinutes)

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
                workoutEfficiencyData = workoutEfficiencyData,
                timeUnderTensionData = timeUnderTensionData,
                energyExpenditureData = energyExpenditureData,
                intraWorkoutProgressionData = intraWorkoutProgressionData,
                loadOverTimeData = loadOverTimeData,
                fatigueProgressionData = fatigueProgressionData,
                repRangeDistributionData = repRangeDistributionData,
                movementPatternsData = movementPatternsData,
                executionQualityData = executionQualityData,
                techniqueQualityData = techniqueQualityData,
                intensityDistributionData = intensityDistributionData,
                rpeAnalysisData = rpeAnalysisData,
                estimated1RMData = estimated1RMData,
                workoutDensityData = workoutDensityData,
            )
        }
    }
}