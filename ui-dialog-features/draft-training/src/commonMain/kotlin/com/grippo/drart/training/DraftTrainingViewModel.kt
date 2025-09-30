package com.grippo.drart.training

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.domain.state.training.toState
import kotlinx.coroutines.flow.onEach

public class DraftTrainingViewModel(
    private val trainingFeature: TrainingFeature
) : BaseViewModel<DraftTrainingState, DraftTrainingDirection, DraftTrainingLoader>(
    DraftTrainingState()
), DraftTrainingContract {

    init {
        trainingFeature.getDraftTraining()
            .onEach(::provideDraftTraining)
            .safeLaunch()
    }

    private fun provideDraftTraining(value: SetDraftTraining?) {
        val exercises = value?.training?.exercises?.toState() ?: return
        update { it.copy(exercises = exercises) }
    }

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
