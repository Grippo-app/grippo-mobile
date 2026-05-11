package com.grippo.performance.trend.details

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.api.user.UserFeature
import com.grippo.domain.state.metrics.performance.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach

public class PerformanceTrendDetailsViewModel(
    range: DateRange,
    metricType: PerformanceMetricTypeState,
    trainingFeature: TrainingFeature,
    private val userFeature: UserFeature,
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
        if (trainings.size < 2) {
            update { it.copy(history = persistentListOf()) }
            return
        }
        val experience = userFeature.observeUser().firstOrNull()?.experience
            ?: ExperienceEnum.BEGINNER
        val history = buildHistory(trainings, experience)
        update { it.copy(history = history) }
    }

    private fun buildHistory(
        trainings: List<Training>,
        experience: ExperienceEnum,
    ): ImmutableList<PerformanceTrendHistoryEntry> {
        val sorted = trainings.sortedBy { it.createdAt }
        val history = mutableListOf<PerformanceTrendHistoryEntry>()

        for (index in sorted.lastIndex downTo 1) {
            if (history.size >= HISTORY_LIMIT) break
            val slice = sorted.subList(0, index + 1)

            val metric = performanceTrendUseCase
                .fromTrainings(trainings = slice, experience = experience)
                .toState()
                .first { it.type == state.value.metricType }

            val endAt = slice.last().createdAt
            history.add(
                PerformanceTrendHistoryEntry(
                    endDate = DateTimeFormatState.of(
                        value = endAt,
                        range = DateRangePresets.infinity(),
                        format = DateFormat.DateOnly.DateMmmDdYyyy,
                    ),
                    metric = metric,
                )
            )
        }

        return history.toPersistentList()
    }

    override fun onBack() {
        navigateTo(PerformanceTrendDetailsDirection.Back)
    }
}
