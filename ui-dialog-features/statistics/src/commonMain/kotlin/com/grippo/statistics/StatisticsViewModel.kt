package com.grippo.statistics

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.metrics.ExerciseDistributionUseCase
import com.grippo.data.features.api.metrics.MuscleLoadingUseCase
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.metrics.toCategoryDistributionState
import com.grippo.domain.state.metrics.toForceTypeDistributionState
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.metrics.toWeightTypeDistributionState
import com.grippo.domain.state.muscles.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.calculation.AnalyticsApi
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class)
public class StatisticsViewModel(
    config: DialogConfig.Statistics,
    muscleFeature: MuscleFeature,
    colorProvider: ColorProvider,
    stringProvider: StringProvider,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingFeature: TrainingFeature,
    private val muscleLoadingUseCase: MuscleLoadingUseCase,
    private val exerciseDistributionUseCase: ExerciseDistributionUseCase,
) : BaseViewModel<StatisticsState, StatisticsDirection, StatisticsLoader>(
    StatisticsState(
        mode = when (config) {
            is DialogConfig.Statistics.Trainings -> StatisticsMode.Trainings(
                range = config.range
            )

            is DialogConfig.Statistics.Training -> StatisticsMode.Exercises()
        }
    )
), StatisticsContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

    init {
        when (config) {
            is DialogConfig.Statistics.Trainings -> observeTrainings(config.range)
            is DialogConfig.Statistics.Training -> observeTraining(config.id)
        }

        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { collectExampleIds(it.mode) }
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExamples)
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        state
            .map { s -> listOf(s.mode, s.examples, s.muscles) }
            .debounce(200)
            .distinctUntilChanged()
            .mapLatest { generateStatistics() }
            .safeLaunch(loader = StatisticsLoader.Charts)
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        update { it.copy(muscles = list.toState()) }
    }

    private fun provideExerciseExamples(list: List<ExerciseExample>) {
        update { it.copy(examples = list.toState()) }
    }

    override fun onBack() {
        navigateTo(StatisticsDirection.Back)
    }

    private fun observeTrainings(range: DateRange) {
        trainingFeature.observeTrainings(range.from, range.to)
            .onEach { provideTrainings(range, it) }
            .safeLaunch()

        safeLaunch {
            trainingFeature.getTrainings(
                start = range.from,
                end = range.to
            ).getOrThrow()
        }
    }

    private fun observeTraining(id: String) {
        trainingFeature.observeTraining(id)
            .onEach(::provideTraining)
            .safeLaunch()
    }

    private suspend fun generateStatistics() {
        val currentState = state.value
        val muscles = currentState.muscles
        val examples = currentState.examples

        when (val mode = currentState.mode) {
            is StatisticsMode.Trainings -> {
                val trainings = mode.trainings
                val range = mode.range

                provideTrainingStatistics(
                    trainings = trainings,
                    range = range,
                    muscles = muscles,
                    examples = examples
                )
            }

            is StatisticsMode.Exercises -> provideExerciseStatistics(
                exercises = mode.exercises,
            )
        }
    }

    private suspend fun provideTrainingStatistics(
        trainings: ImmutableList<TrainingState>,
        range: DateRange,
        muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>>,
        examples: ImmutableList<ExerciseExampleState>,
    ) {
        if (trainings.isEmpty()) {
            clearStatistics()
            return
        }

        val totalMetrics = analytics.metricsFromTrainings(
            trainings = trainings
        )

        val setTrainings = trainings.toDomain()

        val categoryDistribution = exerciseDistributionUseCase
            .categoriesFromTrainings(setTrainings)
            .toCategoryDistributionState()

        val weightTypeDistribution = exerciseDistributionUseCase
            .weightTypesFromTrainings(setTrainings)
            .toWeightTypeDistributionState()

        val forceTypeDistribution = exerciseDistributionUseCase
            .forceTypesFromTrainings(setTrainings)
            .toForceTypeDistributionState()

        val exerciseVolume = analytics.volumeFromTrainings(
            trainings = trainings,
            range = range
        )

        val muscleLoad = muscleLoadingUseCase
            .fromSetTrainings(setTrainings)
            .toState()

        val heatmap = analytics.heatmapFromTrainings(
            trainings = trainings,
            range = range,
            examples = examples,
            groups = muscles
        )

        update {
            it.copy(
                totalMetrics = totalMetrics,
                exerciseVolume = exerciseVolume,
                categoryDistribution = categoryDistribution,
                weightTypeDistribution = weightTypeDistribution,
                forceTypeDistribution = forceTypeDistribution,
                muscleLoad = muscleLoad,
                temporalHeatmap = heatmap,
            )
        }
    }

    private suspend fun provideExerciseStatistics(
        exercises: ImmutableList<ExerciseState>,
    ) {
        if (exercises.isEmpty()) {
            clearStatistics()
            return
        }

        val totalMetrics = analytics.metricsFromExercises(
            exercises = exercises
        )

        val setExercises = exercises.toDomain()

        val categoryDistribution = exerciseDistributionUseCase
            .categoriesFromExercises(setExercises)
            .toCategoryDistributionState()

        val weightTypeDistribution = exerciseDistributionUseCase
            .weightTypesFromExercises(setExercises)
            .toWeightTypeDistributionState()

        val forceTypeDistribution = exerciseDistributionUseCase
            .forceTypesFromExercises(setExercises)
            .toForceTypeDistributionState()

        val exerciseVolume = analytics.volumeFromExercises(
            exercises = exercises
        )
        val muscleLoad = muscleLoadingUseCase
            .fromExercises(setExercises)
            .toState()

        update {
            it.copy(
                totalMetrics = totalMetrics,
                exerciseVolume = exerciseVolume,
                categoryDistribution = categoryDistribution,
                weightTypeDistribution = weightTypeDistribution,
                forceTypeDistribution = forceTypeDistribution,
                muscleLoad = muscleLoad,
                temporalHeatmap = null,
            )
        }
    }

    private fun clearStatistics() {
        update {
            it.copy(
                totalMetrics = null,
                exerciseVolume = null,
                categoryDistribution = null,
                weightTypeDistribution = null,
                forceTypeDistribution = null,
                muscleLoad = null,
                temporalHeatmap = null,
            )
        }
    }

    private fun collectExampleIds(mode: StatisticsMode): List<String> = when (mode) {
        is StatisticsMode.Trainings -> mode.trainings
            .flatMap { training -> training.exercises.map { it.exerciseExample.id } }
            .distinct()

        is StatisticsMode.Exercises -> mode.exercises
            .map { it.exerciseExample.id }
            .distinct()
    }

    private fun provideTrainings(range: DateRange, list: List<Training>) {
        val trainings = list
            .toState()
            .toPersistentList()

        update {
            it.copy(
                mode = StatisticsMode.Trainings(
                    trainings = trainings,
                    range = range
                )
            )
        }
    }

    private fun provideTraining(training: Training?) {
        val exercises = training
            ?.toState()
            ?.exercises
            ?: persistentListOf()

        update {
            it.copy(
                mode = StatisticsMode.Exercises(
                    exercises = exercises
                )
            )
        }
    }

}
