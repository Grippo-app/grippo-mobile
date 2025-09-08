package com.grippo.drart.training

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature

public class DraftTrainingViewModel(
    private val trainingFeature: TrainingFeature
) : BaseViewModel<DraftTrainingState, DraftTrainingDirection, DraftTrainingLoader>(
    DraftTrainingState()
), DraftTrainingContract {

    override fun onContinue() {
        navigateTo(DraftTrainingDirection.Continue)
    }

    override fun onDelete() {
        safeLaunch {
            trainingFeature.deleteDraftTraining().getOrThrow()
            navigateTo(DraftTrainingDirection.Back)
        }
    }

    override fun onBack() {
        navigateTo(DraftTrainingDirection.Back)
    }
}
