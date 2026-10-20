package com.grippo.training.streak.details

import com.grippo.analytics.training.streak.details.TrainingStreakDetailsAnalytics
import com.grippo.analytics.training.streak.details.models.TrainingStreakDetailsEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.data.features.api.metrics.engagement.TrainingStreakUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.engagement.toState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.currentDateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

public class TrainingStreakDetailsViewModel(
    range: DateRange,
    private val analytics: TrainingStreakDetailsAnalytics,
    trainingFeature: TrainingFeature,
    private val trainingStreakUseCase: TrainingStreakUseCase,
) : BaseViewModel<TrainingStreakDetailsDialogState, TrainingStreakDetailsDirection, TrainingStreakDetailsLoader>(
    TrainingStreakDetailsDialogState(range = DateRangeFormatState.of(range))
), TrainingStreakDetailsContract {

    init {
        trainingFeature.observeTrainings(start = range.from, end = range.to)
            .combine(currentDateFlow()) { trainings, _ -> trainings }
            .onEach(::provideTrainingStreak)
            .safeLaunch()
    }

    private fun provideTrainingStreak(trainings: List<Training>) {
        val range = state.value.range.value ?: return
        val streak = trainingStreakUseCase
            .fromTrainings(trainings = trainings, range = range)
            .toState()

        update { it.copy(streak = streak) }
    }

    override fun onBack() {
        analytics.trackEvent(TrainingStreakDetailsEvent.DismissRequested)
        navigateTo(TrainingStreakDetailsDirection.Back)
    }
}
