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
                    onResult = { navigateTo(HomeRootDirection.DraftTraining) }
                )

                dialogController.show(config)
            }
        }
    }

    override fun onBack() {
        navigateTo(HomeRootDirection.Back)
    }

    override fun toExcludedMuscles() {
        navigateTo(HomeRootDirection.ExcludedMuscles)
    }

    override fun toMissingEquipment() {
        navigateTo(HomeRootDirection.MissingEquipment)
    }

    override fun toWeightHistory() {
        navigateTo(HomeRootDirection.WeightHistory)
    }

    override fun toExperience() {
        navigateTo(HomeRootDirection.Experience)
    }

    override fun toDebug() {
        navigateTo(HomeRootDirection.Debug)
    }

    override fun toTrainings() {
        navigateTo(HomeRootDirection.Trainings)
    }

    override fun toAddTraining() {
        navigateTo(HomeRootDirection.AddTraining)
    }

    override fun toSettings() {
        navigateTo(HomeRootDirection.Settings)
    }
}
