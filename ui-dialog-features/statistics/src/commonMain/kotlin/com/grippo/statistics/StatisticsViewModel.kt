package com.grippo.statistics

import com.grippo.analytics.statistics.StatisticsAnalytics
import com.grippo.analytics.statistics.models.StatisticsEvent
import com.grippo.analytics.statistics.models.StatisticsOperation
import com.grippo.core.foundation.BaseViewModel
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
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

public class StatisticsViewModel(
    config: DialogConfig.Statistics,
    private val analytics: StatisticsAnalytics,
    private val trainingFeature: TrainingFeature,
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
                trainingFeature
                    .observeTrainings(config.range.from, config.range.to)
                    .flatMapLatest { trainings ->
                        observeTrainingStatistics(range = config.range, trainings = trainings)
                    }
                    .buffer(0)
                    .onEach(::provideMuscleLoad)
                    .safeLaunch()

                safeLaunch {
                    withLoader(StatisticsLoader.Charts) {
                        analytics.trackOperation(StatisticsOperation.RefreshTrainings) {
                            trainingFeature
                                .getTrainings(start = config.range.from, end = config.range.to)
                                .getOrThrow()
                        }
                    }
                }
            }

            is DialogConfig.Statistics.Training -> {
                trainingFeature.observeTraining(config.id)
                    .flatMapLatest(::observeExerciseStatistics)
                    .buffer(0)
                    .onEach(::provideMuscleLoad)
                    .safeLaunch()
            }
        }
    }

    private fun observeTrainingStatistics(
        range: DateRange,
        trainings: List<Training>,
    ): Flow<MuscleLoadSummary> {
        provideTrainingStatistics(range = range, trainings = trainings)

        return if (trainings.isEmpty()) {
            emptyFlow()
        } else {
            muscleLoadingSummaryUseCase.observeFromTrainings(trainings)
        }
    }

    private fun observeExerciseStatistics(training: Training?): Flow<MuscleLoadSummary> {
        provideExerciseStatistics(training)

        return if (training == null) {
            emptyFlow()
        } else {
            muscleLoadingSummaryUseCase.observeFromExercises(training.exercises)
        }
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

        update {
            it.copy(
                mode = mode,
                exerciseVolume = volume,
                total = totalMetrics,
                muscleLoad = null,
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

        update {
            it.copy(
                mode = StatisticsMode.Exercises,
                exerciseVolume = volume,
                total = totalMetrics,
                muscleLoad = null,
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
