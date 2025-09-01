package com.grippo.training.recording

import com.grippo.core.BaseViewModel
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSRadarData
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.uuid.Uuid

internal class TrainingRecordingViewModel(
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState()
), TrainingRecordingContract {

    private val statisticsCalculator = TrainingStatisticsCalculator(stringProvider)

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
        val colors = colorProvider.get()

        val exercises = state.value.exercises

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
                    muscleGroupBalanceData = DSRadarData(axes = emptyList(), series = emptyList()),
                    workoutEfficiencyData = DSProgressData(items = emptyList()),
                    timeUnderTensionData = DSProgressData(items = emptyList()),
                    energyExpenditureData = DSProgressData(items = emptyList()),
                    intraWorkoutProgressionData = DSAreaData(points = emptyList()),
                    loadOverTimeData = DSAreaData(points = emptyList()),
                    fatigueProgressionData = DSAreaData(points = emptyList()),
                    pushPullBalanceData = DSPieData(slices = emptyList()),
                    repRangeDistributionData = DSPieData(slices = emptyList()),
                    movementPatternsData = DSPieData(slices = emptyList()),
                    executionQualityData = DSProgressData(items = emptyList()),
                    techniqueQualityData = DSSparklineData(points = emptyList()),
                    weakPointsData = DSProgressData(items = emptyList()),
                    intensityDistributionData = DSBarData(items = emptyList()),
                    rpeAnalysisData = DSBarData(items = emptyList()),
                    estimated1RMData = DSBarData(items = emptyList()),
                    workoutDensityData = DSSparklineData(points = emptyList()),
                    exerciseTypeDistributionData = DSBarData(items = emptyList())
                )
            }
            return
        }

        val workoutDurationMinutes = exercises.size * 15

        val totalMetrics = statisticsCalculator
            .calculateTotalMetrics(exercises)

        val exerciseVolumeData = statisticsCalculator
            .calculateExerciseVolumeChart(exercises, colors)

        val categoryDistributionData = statisticsCalculator
            .calculateCategoryDistribution(exercises, colors)
        val weightTypeDistributionData = statisticsCalculator
            .calculateWeightTypeDistribution(exercises, colors)
        val forceTypeDistributionData = statisticsCalculator
            .calculateForceTypeDistribution(exercises, colors)
        val experienceDistributionData = statisticsCalculator
            .calculateExperienceDistribution(exercises, colors)

        statisticsCalculator
            .calculateMuscleLoadDistribution(exercises, colors)

        statisticsCalculator
            .calculateMuscleGroupBalance(exercises, colors)

        statisticsCalculator
            .calculateWorkoutEfficiency(exercises, workoutDurationMinutes, colors)

        statisticsCalculator
            .calculateTimeUnderTension(exercises, colors)

        statisticsCalculator // 75kg default weight
            .calculateEnergyExpenditure(exercises, 75f, colors)

        statisticsCalculator
            .calculateIntraWorkoutProgression(exercises, colors)
        statisticsCalculator
            .calculateLoadOverTime(exercises, colors)
        statisticsCalculator
            .calculateFatigueProgression(exercises, colors)

        statisticsCalculator
            .calculatePushPullBalance(exercises, colors)
        statisticsCalculator
            .calculateRepRangeDistribution(exercises, colors)
        statisticsCalculator
            .calculateMovementPatterns(exercises, colors)

        statisticsCalculator
            .calculateExecutionQuality(exercises, colors)
        statisticsCalculator
            .calculateTechniqueQuality(exercises, colors)
        statisticsCalculator
            .calculateWeakPoints(exercises, colors)

        statisticsCalculator
            .calculateIntensityDistribution(exercises, colors)
        statisticsCalculator
            .calculateRPEAnalysis(exercises, colors)
        statisticsCalculator
            .calculateEstimated1RM(exercises, colors)
        statisticsCalculator
            .calculateWorkoutDensity(exercises, workoutDurationMinutes, colors)

        statisticsCalculator
            .calculateExerciseTypeDistribution(exercises, colors)

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


//                muscleLoadData = muscleLoadData,
//                muscleGroupBalanceData = muscleGroupBalanceData,
//                workoutEfficiencyData = workoutEfficiencyData,
//                timeUnderTensionData = timeUnderTensionData,
//                energyExpenditureData = energyExpenditureData,
//                intraWorkoutProgressionData = intraWorkoutProgressionData,
//                loadOverTimeData = loadOverTimeData,
//                fatigueProgressionData = fatigueProgressionData,
//                pushPullBalanceData = pushPullBalanceData,
//                repRangeDistributionData = repRangeDistributionData,
//                movementPatternsData = movementPatternsData,
//                executionQualityData = executionQualityData,
//                techniqueQualityData = techniqueQualityData,
//                weakPointsData = weakPointsData,
//                intensityDistributionData = intensityDistributionData,
//                rpeAnalysisData = rpeAnalysisData,
//                estimated1RMData = estimated1RMData,
//                workoutDensityData = workoutDensityData,
//                exerciseTypeDistributionData = exerciseTypeDistributionData
            )
        }
    }
}