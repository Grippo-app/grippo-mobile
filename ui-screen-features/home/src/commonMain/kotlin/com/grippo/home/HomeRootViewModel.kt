package com.grippo.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import kotlinx.coroutines.flow.firstOrNull

public class HomeRootViewModel(
    trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
) : BaseViewModel<HomeRootState, HomeRootDirection, HomeRootLoader>(
    HomeRootState
), HomeRootContract {

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
                    onResult = { navigateTo(HomeRootDirection.ToDraftTraining) }
                )

                dialogController.show(config)
            }
        }
    }

    override fun onBack() {
        navigateTo(HomeRootDirection.Back)
    }

    override fun toExcludedMuscles() {
        navigateTo(HomeRootDirection.ToExcludedMuscles)
    }

    override fun toMissingEquipment() {
        navigateTo(HomeRootDirection.ToMissingEquipment)
    }

    override fun toWeightHistory() {
        navigateTo(HomeRootDirection.ToWeightHistory)
    }

    override fun toDebug() {
        navigateTo(HomeRootDirection.ToDebug)
    }

    override fun toAddTraining() {
        navigateTo(HomeRootDirection.ToAddTraining)
    }
}
