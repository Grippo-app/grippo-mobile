package com.grippo.performance.trend.details

import com.grippo.analytics.performance.trend.details.PerformanceTrendDetailsAnalytics
import com.grippo.analytics.performance.trend.details.models.PerformanceTrendDetailsEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.performance.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class PerformanceTrendDetailsViewModel(
    range: DateRange,
    metricType: PerformanceMetricTypeState,
    private val analytics: PerformanceTrendDetailsAnalytics,
    trainingFeature: TrainingFeature,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
) : BaseViewModel<PerformanceTrendDetailsDialogState, PerformanceTrendDetailsDirection, PerformanceTrendDetailsLoader>(
    PerformanceTrendDetailsDialogState(
        range = DateRangeFormatState.of(range),
        metricType = metricType
    )
), PerformanceTrendDetailsContract {

    init {
        trainingFeature.observeTrainings(start = range.from, end = range.to)
            .onEach(::providePerformance)
            .safeLaunch()
    }

    private fun providePerformance(trainings: List<Training>) {
        val sorted = trainings.sortedWith(compareBy<Training> { it.createdAt }.thenBy { it.id })
        val history = sorted.indices.reversed().take(HISTORY_LIMIT).map { index ->
            val training = sorted[index]
            val metric = performanceTrendUseCase
                .fromTrainings(trainings = sorted.subList(0, index + 1))
                .toState()
                .first { it.type == state.value.metricType }

            PerformanceTrendHistoryEntry(
                trainingId = training.id,
                endDate = DateTimeFormatState.of(
                    value = training.createdAt,
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
                metric = metric,
            )
        }.toPersistentList()

        update { it.copy(history = history) }
    }

    override fun onBack() {
        analytics.trackEvent(PerformanceTrendDetailsEvent.DismissRequested)
        navigateTo(PerformanceTrendDetailsDirection.Back)
    }

    private companion object {
        const val HISTORY_LIMIT = 10
    }
}
