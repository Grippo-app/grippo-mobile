package com.grippo.training.streak

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.metrics.TrainingStreakUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.onEach

public class TrainingStreakViewModel(
    range: DateRange,
    trainingFeature: TrainingFeature,
    private val trainingStreakUseCase: TrainingStreakUseCase,
) : BaseViewModel<TrainingStreakDialogState, TrainingStreakDirection, TrainingStreakLoader>(
    TrainingStreakDialogState(range = range)
), TrainingStreakContract {

    init {
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::provideTrainingStreak)
            .safeLaunch(loader = TrainingStreakLoader.Content)
    }

    override fun onBack() {
        navigateTo(TrainingStreakDirection.Back)
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
