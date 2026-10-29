package com.grippo.statistics

import com.grippo.analytics.statistics.StatisticsAnalytics
import com.grippo.analytics.statistics.models.StatisticsEvent
import com.grippo.analytics.statistics.models.StatisticsOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadSummary
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.metrics.volume.VolumeSeriesUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

public class StatisticsViewModel(
    config: DialogConfig.Statistics,
    private val analytics: StatisticsAnalytics,
    trainingFeature: TrainingFeature,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val volumeSeriesUseCase: VolumeSeriesUseCase,
    private val trainingTotalUseCase: TrainingTotalUseCase,
) : BaseViewModel<StatisticsState, StatisticsDirection, StatisticsLoader>(
    StatisticsState(
        mode = when (config) {
            is DialogConfig.Statistics.Trainings -> StatisticsMode.Trainings(
                range = DateRangeFormatState.of(config.range)
            )

            is DialogConfig.Statistics.Training -> StatisticsMode.Exercises
        }
    )
), StatisticsContract {

    init {
        when (config) {
            is DialogConfig.Statistics.Trainings -> {
                trainingFeature.observeTrainings(start = config.range.from, end = config.range.to)
                    .onEach { trainings -> provideTrainingStatistics(range = config.range, trainings = trainings) }
                    .flatMapLatest(::observeTrainingMuscleLoad)
                    .safeLaunch(recovery = ErrorRecovery.Retry)

                safeLaunch(loader = StatisticsLoader.Charts, recovery = ErrorRecovery.Retry) {
                    analytics.trackOperation(StatisticsOperation.RefreshTrainings) {
                        trainingFeature.getTrainings(start = config.range.from, end = config.range.to)
                    }
                }
            }

            is DialogConfig.Statistics.Training -> {
                trainingFeature.observeTraining(config.id)
                    .onEach(::provideExerciseStatistics)
                    .flatMapLatest(::observeExerciseMuscleLoad)
                    .safeLaunch(recovery = ErrorRecovery.Retry)
            }
        }
    }

    private fun observeTrainingMuscleLoad(trainings: List<Training>): Flow<MuscleLoadSummary> {
        if (trainings.isEmpty()) {
            return emptyFlow()
        }

        return muscleLoadingSummaryUseCase.observeFromTrainings(trainings)
            .onEach(::provideMuscleLoad)
    }

    private fun observeExerciseMuscleLoad(training: Training?): Flow<MuscleLoadSummary> {
        if (training == null) {
            return emptyFlow()
        }

        return muscleLoadingSummaryUseCase.observeFromExercises(training.exercises)
            .onEach(::provideMuscleLoad)
    }

    private fun provideTrainingStatistics(range: DateRange, trainings: List<Training>) {
        val formattedRange = DateRangeFormatState.of(range)

        val mode = StatisticsMode.Trainings(range = formattedRange)

        val totalMetrics = trainings.takeIf { it.isNotEmpty() }?.let { values ->
            trainingTotalUseCase
                .fromTrainings(values)
                .toState()
        }

        val volume = trainings.takeIf { it.isNotEmpty() }?.let { values ->
            volumeSeriesUseCase
                .fromTrainings(values)
                .toState()
        }

        update { current ->
            current.copy(
                mode = mode,
                total = totalMetrics,
                exerciseVolume = volume,
                muscleLoad = if (totalMetrics == null) null else current.muscleLoad,
            )
        }
    }

    private fun provideExerciseStatistics(training: Training?) {
        val totalMetrics = training?.let { value ->
            trainingTotalUseCase
                .fromExercises(value.exercises)
                .toState()
        }

        val volume = training?.let { value ->
            volumeSeriesUseCase
                .fromExercises(value.exercises)
                .toState()
        }

        update { current ->
            current.copy(
                mode = StatisticsMode.Exercises,
                total = totalMetrics,
                exerciseVolume = volume,
                muscleLoad = if (totalMetrics == null) null else current.muscleLoad,
            )
        }
    }

    private fun provideMuscleLoad(value: MuscleLoadSummary) {
        val muscleLoad = value.toState()

        update { current ->
            if (current.total == null) {
                return@update current
            }

            current.copy(muscleLoad = muscleLoad)
        }
    }

    override fun onBack() {
        analytics.trackEvent(StatisticsEvent.DismissRequested)
        navigateTo(StatisticsDirection.Back)
    }
}
