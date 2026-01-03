package com.grippo.statistics

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.metrics.ExerciseDistributionUseCase
import com.grippo.data.features.api.metrics.MuscleLoadingUseCase
import com.grippo.data.features.api.metrics.MuscleLoadTimelineUseCase
import com.grippo.data.features.api.metrics.TrainingTotalUseCase
import com.grippo.data.features.api.metrics.VolumeSeriesUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.domain.state.metrics.toCategoryDistributionState
import com.grippo.domain.state.metrics.toForceTypeDistributionState
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.metrics.toWeightTypeDistributionState
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.training.toDomain
import com.grippo.state.domain.training.toDomainTrainings
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class)
public class StatisticsViewModel(
    config: DialogConfig.Statistics,
    private val trainingFeature: TrainingFeature,
    private val muscleLoadingUseCase: MuscleLoadingUseCase,
    private val exerciseDistributionUseCase: ExerciseDistributionUseCase,
    private val volumeSeriesUseCase: VolumeSeriesUseCase,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val muscleLoadTimelineUseCase: MuscleLoadTimelineUseCase,
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

    init {
        when (config) {
            is DialogConfig.Statistics.Trainings -> observeTrainings(config.range)
            is DialogConfig.Statistics.Training -> observeTraining(config.id)
        }

        state
            .map { it.mode }
            .debounce(200)
            .distinctUntilChanged()
            .mapLatest { _ -> generateStatistics() }
            .safeLaunch(loader = StatisticsLoader.Charts)
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
        when (val mode = state.value.mode) {
            is StatisticsMode.Trainings -> {
                provideTrainingStatistics(
                    trainings = mode.trainings,
                    range = mode.range,
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
    ) {
        if (trainings.isEmpty()) {
            clearStatistics()
            return
        }

        val setTrainings = trainings.toDomain()
        val domainTrainings = trainings.toDomainTrainings()
        val totalMetrics = trainingTotalUseCase
            .fromTrainings(setTrainings)
            .toState()

        val categoryDistribution = exerciseDistributionUseCase
            .categoriesFromTrainings(setTrainings)
            .toCategoryDistributionState()

        val weightTypeDistribution = exerciseDistributionUseCase
            .weightTypesFromTrainings(setTrainings)
            .toWeightTypeDistributionState()

        val forceTypeDistribution = exerciseDistributionUseCase
            .forceTypesFromTrainings(setTrainings)
            .toForceTypeDistributionState()

        val muscleLoad = muscleLoadingUseCase
            .fromSetTrainings(setTrainings)
            .toState()

        val heatmap = domainTrainings
            .takeIf { it.isNotEmpty() }
            ?.let { list ->
                muscleLoadTimelineUseCase
                    .fromTrainings(list, range)
                    ?.toState()
            }

        update {
            it.copy(
                total = totalMetrics,
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

        val setExercises = exercises.toDomain()
        val totalMetrics = trainingTotalUseCase
            .fromExercises(setExercises)
            .toState()

        val categoryDistribution = exerciseDistributionUseCase
            .categoriesFromExercises(setExercises)
            .toCategoryDistributionState()

        val weightTypeDistribution = exerciseDistributionUseCase
            .weightTypesFromExercises(setExercises)
            .toWeightTypeDistributionState()

        val forceTypeDistribution = exerciseDistributionUseCase
            .forceTypesFromExercises(setExercises)
            .toForceTypeDistributionState()

        val muscleLoad = muscleLoadingUseCase
            .fromExercises(setExercises)
            .toState()

        update {
            it.copy(
                total = totalMetrics,
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
                total = null,
                exerciseVolume = null,
                categoryDistribution = null,
                weightTypeDistribution = null,
                forceTypeDistribution = null,
                muscleLoad = null,
                temporalHeatmap = null,
            )
        }
    }

    private fun provideTrainings(range: DateRange, list: List<Training>) {
        val trainings = list.toState().toPersistentList()
        val volume = volumeSeriesUseCase
            .fromTrainings(list)
            .toState()

        update { current ->
            current.copy(
                mode = StatisticsMode.Trainings(
                    trainings = trainings,
                    range = range
                ),
                exerciseVolume = volume
            )
        }
    }

    private fun provideTraining(training: Training?) {
        val exercises = training
            ?.toState()
            ?.exercises
            ?: persistentListOf()

        val volume = training
            ?.let { value -> volumeSeriesUseCase.fromExercises(value.exercises).toState() }

        update {
            it.copy(
                mode = StatisticsMode.Exercises(
                    exercises = exercises
                ),
                exerciseVolume = volume
            )
        }
    }
}
