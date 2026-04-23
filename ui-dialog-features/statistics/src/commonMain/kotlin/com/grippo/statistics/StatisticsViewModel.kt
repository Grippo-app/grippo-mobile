package com.grippo.statistics

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.metrics.volume.VolumeSeriesUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.mapLatest

public class StatisticsViewModel(
    config: DialogConfig.Statistics,
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
                    .mapLatest { trainings -> provideTrainingStatistics(config.range, trainings) }
                    .safeLaunch(loader = StatisticsLoader.Charts)

                safeLaunch {
                    trainingFeature
                        .getTrainings(start = config.range.from, end = config.range.to)
                        .getOrThrow()
                }
            }

            is DialogConfig.Statistics.Training -> {
                trainingFeature.observeTraining(config.id)
                    .mapLatest { training -> provideExerciseStatistics(training) }
                    .safeLaunch(loader = StatisticsLoader.Charts)
            }
        }
    }

    override fun onBack() {
        navigateTo(StatisticsDirection.Back)
    }

    private suspend fun provideTrainingStatistics(range: DateRange, trainings: List<Training>) {
        val formattedRange = DateRangeFormatState.of(range)

        if (trainings.isEmpty()) {
            clearStatistics(mode = StatisticsMode.Trainings(formattedRange))
            return
        }

        val totalMetrics = trainingTotalUseCase
            .fromTrainings(trainings)
            .toState()

        val muscleLoad = muscleLoadingSummaryUseCase
            .fromTrainings(trainings)
            .toState()

        val volume = volumeSeriesUseCase
            .fromTrainings(trainings)
            .toState()

        update {
            it.copy(
                mode = StatisticsMode.Trainings(range = formattedRange),
                exerciseVolume = volume,
                total = totalMetrics,
                muscleLoad = muscleLoad,
            )
        }
    }

    private suspend fun provideExerciseStatistics(training: Training?) {
        if (training == null) {
            clearStatistics(mode = StatisticsMode.Exercises)
            return
        }

        val totalMetrics = trainingTotalUseCase
            .fromExercises(training.exercises)
            .toState()

        val muscleLoad = muscleLoadingSummaryUseCase
            .fromExercises(training.exercises)
            .toState()

        val volume = volumeSeriesUseCase
            .fromExercises(training.exercises)
            .toState()

        update {
            it.copy(
                mode = StatisticsMode.Exercises,
                exerciseVolume = volume,
                total = totalMetrics,
                muscleLoad = muscleLoad,
            )
        }
    }

    private fun clearStatistics(mode: StatisticsMode? = null) {
        update {
            it.copy(
                mode = mode ?: it.mode,
                total = null,
                exerciseVolume = null,
                muscleLoad = null,
            )
        }
    }
}
