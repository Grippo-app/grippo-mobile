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
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::provideTrainingStreak)
            .safeLaunch()
    }

    override fun onBack() {
        analytics.trackEvent(TrainingStreakDetailsEvent.DismissRequested)
        navigateTo(TrainingStreakDetailsDirection.Back)
    }

    private suspend fun provideTrainingStreak(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(streak = null) }
            return
        }

        val streak = trainingStreakUseCase
            .fromTrainings(trainings)
            .toState()

        update { it.copy(streak = streak) }
    }
}
