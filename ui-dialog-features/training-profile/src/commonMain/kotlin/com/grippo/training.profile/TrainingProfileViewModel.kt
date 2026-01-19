package com.grippo.training.profile

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.metrics.TrainingLoadProfileUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.onEach

public class TrainingProfileViewModel(
    range: DateRange,
    trainingFeature: TrainingFeature,
    private val trainingLoadProfileUseCase: TrainingLoadProfileUseCase,
) : BaseViewModel<TrainingProfileDialogState, TrainingProfileDirection, TrainingProfileLoader>(
    TrainingProfileDialogState(range = range)
), TrainingProfileContract {

    init {
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::provideTrainingProfile)
            .safeLaunch(loader = TrainingProfileLoader.Content)
    }

    override fun onBack() {
        navigateTo(TrainingProfileDirection.Back)
    }

    private suspend fun provideTrainingProfile(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(profile = null) }
            return
        }

        val profile = trainingLoadProfileUseCase
            .fromTrainings(trainings)
            .toState()

        update { it.copy(profile = profile) }
    }
}
