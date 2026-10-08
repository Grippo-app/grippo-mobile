package com.grippo.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dialog_saved_workout
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import kotlinx.coroutines.flow.firstOrNull

public class TrainingsRootViewModel(
    trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
) : BaseViewModel<TrainingsRootState, TrainingsRootDirection, TrainingsRootLoader>(
    TrainingsRootState
), TrainingsRootContract {

    init {
        safeLaunch {
            val training = trainingFeature.getDraftTraining().firstOrNull()
            provideDraftTraining(training)
        }
    }

    private fun provideDraftTraining(value: DraftTraining?) {
        val hasDraftTraining = value != null

        if (hasDraftTraining) {
            val config = DialogConfig.DraftTraining(
                title = UiText.Res(Res.string.dialog_saved_workout),
                onContinue = { navigateTo(TrainingsRootDirection.DraftTraining) }
            )

            dialogController.open(config)
        }
    }

    override fun onBack() {
        navigateTo(TrainingsRootDirection.Back)
    }

    override fun toEditTraining(id: String) {
        navigateTo(TrainingsRootDirection.EditTraining(id))
    }

    override fun toStartTraining() {
        navigateTo(TrainingsRootDirection.StartTraining)
    }
}
