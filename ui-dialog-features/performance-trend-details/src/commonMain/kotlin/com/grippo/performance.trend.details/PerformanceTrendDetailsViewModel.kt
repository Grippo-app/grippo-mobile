package com.grippo.performance.trend.details

import com.grippo.analytics.performance.trend.details.PerformanceTrendDetailsAnalytics
import com.grippo.analytics.performance.trend.details.models.PerformanceTrendDetailsEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.performance.toState
import com.grippo.state.domain.metrics.performance.toDomain
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class PerformanceTrendDetailsViewModel(
    range: DateRange,
    metricType: PerformanceMetricTypeState,
    reference: PerformanceReferenceState,
    private val analytics: PerformanceTrendDetailsAnalytics,
    trainingFeature: TrainingFeature,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
) : BaseViewModel<PerformanceTrendDetailsDialogState, PerformanceTrendDetailsDirection, PerformanceTrendDetailsLoader>(
    PerformanceTrendDetailsDialogState(
        range = DateRangeFormatState.of(range),
        metricType = metricType,
        reference = reference,
    )
), PerformanceTrendDetailsContract {

    init {
        trainingFeature.observeTrainings(start = range.from, end = range.to)
            .onEach(::providePerformance)
            .safeLaunch(recovery = ErrorRecovery.Retry)
    }

    private fun providePerformance(trainings: List<Training>) {
        val sorted = trainings.sortedWith(compareBy<Training> { it.createdAt }.thenBy { it.id })

        val reference = state.value.reference.toDomain()

        val history = sorted.indices.reversed().take(HISTORY_LIMIT).map { index ->
            val training = sorted[index]
            val trainingsSoFar = sorted.subList(fromIndex = 0, toIndex = index + 1)
            val metric = performanceTrendUseCase
                .fromTrainings(trainings = trainingsSoFar, reference = reference)
                .toState()
                .first { it.type == state.value.metricType }

            PerformanceTrendHistoryEntry(
                trainingId = training.id,
                endDate = DateTimeFormatState.of(
                    value = training.createdAt,
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateDdMmmYyyy,
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
