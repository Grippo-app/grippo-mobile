package com.grippo.performance.trend

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.PerformanceTrendHistoryEntry
import com.grippo.data.features.api.metrics.PerformanceTrendUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class PerformanceTrendViewModel(
    range: DateRange,
    metricType: PerformanceMetricTypeState,
    trainingFeature: TrainingFeature,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
) : BaseViewModel<PerformanceTrendDialogState, PerformanceTrendDirection, PerformanceTrendLoader>(
    PerformanceTrendDialogState(range = range, metricType = metricType)
), PerformanceTrendContract {

    private companion object {
        private const val HISTORY_LIMIT = 6
    }

    init {
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::providePerformance)
            .safeLaunch(loader = PerformanceTrendLoader.Content)
    }

    private suspend fun providePerformance(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(chartPoints = persistentListOf(), history = persistentListOf()) }
            return
        }

        val metrics = performanceTrendUseCase
            .fromTrainings(trainings)
            .toState()

        val metric = metrics.firstOrNull { it.type == state.value.metricType }

        val chartPoints = if (metric == null) {
            persistentListOf()
        } else {
            buildChartPoints(trainings, state.value.metricType)
        }
        val history = if (metric == null) {
            persistentListOf()
        } else {
            buildHistory(trainings, state.value.metricType, metric)
        }

        update { it.copy(chartPoints = chartPoints, history = history) }
    }

    private fun buildChartPoints(
        trainings: List<Training>,
        type: PerformanceMetricTypeState,
    ): ImmutableList<Float> {
        val values = trainings
            .sortedBy { it.createdAt }
            .mapNotNull { training -> valueOf(training, type) }

        if (values.size < 2) {
            return persistentListOf()
        }

        return values.toPersistentList()
    }

    private suspend fun buildHistory(
        trainings: List<Training>,
        type: PerformanceMetricTypeState,
        currentMetric: PerformanceMetricState,
    ): ImmutableList<PerformanceTrendHistoryEntry> {
        if (trainings.size < 2) return persistentListOf()
        val sorted = trainings.sortedBy { it.createdAt }
        val history = mutableListOf<PerformanceTrendHistoryEntry>()

        history.add(
            PerformanceTrendHistoryEntry(
                range = state.value.range,
                metric = currentMetric
            )
        )

        for (index in sorted.lastIndex - 1 downTo 1) {
            if (history.size >= HISTORY_LIMIT) break
            val slice = sorted.subList(0, index + 1)

            val metric = performanceTrendUseCase
                .fromTrainings(slice)
                .toState()
                .firstOrNull { it.type == type }

            if (metric != null) {
                history.add(
                    PerformanceTrendHistoryEntry(
                        range = DateRange(
                            from = slice.first().createdAt,
                            to = slice.last().createdAt
                        ),
                        metric = metric
                    )
                )
            }
        }

        return history.toPersistentList()
    }

    private fun valueOf(training: Training, type: PerformanceMetricTypeState): Float? {
        return when (type) {
            PerformanceMetricTypeState.Duration -> {
                val minutes = training.duration.inWholeSeconds.toFloat() / 60f
                minutes.takeIf { it > 0f }
            }

            PerformanceMetricTypeState.Volume -> {
                training.volume.takeIf { it > 0f }
            }

            PerformanceMetricTypeState.Density -> {
                val minutes = training.duration.inWholeSeconds.toFloat() / 60f
                if (minutes <= 0f) return null
                val volume = training.volume
                if (volume <= 0f) return null
                (volume / minutes).takeIf { it > 0f }
            }

            PerformanceMetricTypeState.Repetitions -> {
                val repetitions = training.repetitions.toFloat()
                repetitions.takeIf { it > 0f }
            }

            PerformanceMetricTypeState.Intensity -> {
                training.intensity.takeIf { it > 0f }
            }
        }
    }

    override fun onBack() {
        navigateTo(PerformanceTrendDirection.Back)
    }
}
