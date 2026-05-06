package com.grippo.draft.training

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
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

    private fun provideDraftTraining(value: DraftTraining?) {
        value ?: return
        update {
            it.copy(
                exercises = value.exercises.toState(),
                stage = when (val trainingId = value.trainingId) {
                    null -> StageState.Add
                    else -> StageState.Edit(trainingId)
                }
            )
        }
    }

    override fun onContinue() {
        navigateTo(DraftTrainingDirection.Continue)
    }

    override fun onStartNew() {
        safeLaunch {
            trainingFeature.deleteDraftTraining().getOrThrow()
            navigateTo(DraftTrainingDirection.StartNew)
        }
    }

    override fun onBack() {
        navigateTo(DraftTrainingDirection.Back)
    }
}
