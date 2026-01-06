package com.grippo.muscle.loading

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.metrics.MuscleLoadingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.onEach

public class MuscleLoadingViewModel(
    range: DateRange,
    trainingFeature: TrainingFeature,
    private val muscleLoadingUseCase: MuscleLoadingUseCase,
) : BaseViewModel<MuscleLoadingState, MuscleLoadingDirection, MuscleLoadingLoader>(
    MuscleLoadingState(range = range)
), MuscleLoadingContract {

    init {
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::provideMuscleLoad)
            .safeLaunch(loader = MuscleLoadingLoader.Content)
    }

    override fun onBack() {
        navigateTo(MuscleLoadingDirection.Back)
    }

    private suspend fun provideMuscleLoad(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(summary = null) }
            return
        }

        val summary = muscleLoadingUseCase
            .fromTrainings(trainings)
            .toState()

        update { it.copy(summary = summary) }
    }

    override fun onSelectMode(value: MuscleLoadingShowingMode) {
        update { it.copy(mode = value) }
    }
}
