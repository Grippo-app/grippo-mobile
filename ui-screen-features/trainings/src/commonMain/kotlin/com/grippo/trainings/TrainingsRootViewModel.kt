package com.grippo.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
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

    private fun provideDraftTraining(value: SetDraftTraining?) {
        val hasDraftTraining = value != null

        if (hasDraftTraining) {
            safeLaunch {
                val config = DialogConfig.DraftTraining(
                    onResult = { navigateTo(TrainingsRootDirection.ToDraftTraining) }
                )

                dialogController.show(config)
            }
        }
    }

    override fun onBack() {
        navigateTo(TrainingsRootDirection.Back)
    }

    override fun toExcludedMuscles() {
        navigateTo(TrainingsRootDirection.ToExcludedMuscles)
    }

    override fun toMissingEquipment() {
        navigateTo(TrainingsRootDirection.ToMissingEquipment)
    }

    override fun toWeightHistory() {
        navigateTo(TrainingsRootDirection.ToWeightHistory)
    }

    override fun toDebug() {
        navigateTo(TrainingsRootDirection.ToDebug)
    }

    override fun toEditTraining(id: String) {
        navigateTo(TrainingsRootDirection.ToEditTraining(id))
    }

    override fun toAddTraining() {
        navigateTo(TrainingsRootDirection.ToAddTraining)
    }
}
