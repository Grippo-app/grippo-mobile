package com.grippo.performance.trend.details

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.PerformanceTrendHistoryEntry
import com.grippo.data.features.api.metrics.PerformanceTrendUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class PerformanceTrendDetailsViewModel(
    range: DateRange,
    metricType: PerformanceMetricTypeState,
    trainingFeature: TrainingFeature,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
) : BaseViewModel<PerformanceTrendDetailsDialogState, PerformanceTrendDetailsDirection, PerformanceTrendDetailsLoader>(
    PerformanceTrendDetailsDialogState(
        range = DateRangeFormatState.of(range),
        metricType = metricType
    )
), PerformanceTrendDetailsContract {

    private companion object {
        private const val HISTORY_LIMIT = 10
    }

    init {
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::providePerformance)
            .safeLaunch()
    }

    private suspend fun providePerformance(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(history = persistentListOf()) }
            return
        }

        val history = buildHistory(trainings)
        update { it.copy(history = history) }
    }

    private suspend fun buildHistory(trainings: List<Training>): ImmutableList<PerformanceTrendHistoryEntry> {
        if (trainings.size < 2) return persistentListOf()
        val sorted = trainings.sortedBy { it.createdAt }
        val history = mutableListOf<PerformanceTrendHistoryEntry>()

        for (index in sorted.lastIndex downTo 1) {
            if (history.size >= HISTORY_LIMIT) break
            val slice = sorted.subList(0, index + 1)

            val metric = performanceTrendUseCase
                .fromTrainings(slice)
                .toState()
                .firstOrNull { it.type == state.value.metricType }

            if (metric != null) {
                val endAt = slice.last().createdAt

                history.add(
                    PerformanceTrendHistoryEntry(
                        endDate = DateFormatState.of(
                            value = endAt,
                            range = DateRangePresets.infinity(),
                            format = DateFormat.DateOnly.DateMmmDdYyyy,
                        ),
                        metric = metric
                    )
                )
            }
        }

        return history.toPersistentList()
    }

    override fun onBack() {
        navigateTo(PerformanceTrendDetailsDirection.Back)
    }
}
