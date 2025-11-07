package com.grippo.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import kotlinx.coroutines.flow.firstOrNull

public class HomeViewModel(
    trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(HomeState), HomeContract {

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
                    onResult = { navigateTo(HomeDirection.ToDraftTraining) }
                )

                dialogController.show(config)
            }
        }
    }

    override fun onBack() {
        navigateTo(HomeDirection.Back)
    }

    override fun toExcludedMuscles() {
        navigateTo(HomeDirection.ToExcludedMuscles)
    }

    override fun toMissingEquipment() {
        navigateTo(HomeDirection.ToMissingEquipment)
    }

    override fun toWeightHistory() {
        navigateTo(HomeDirection.ToWeightHistory)
    }

    override fun toDebug() {
        navigateTo(HomeDirection.ToDebug)
    }

    override fun toEditTraining(id: String) {
        navigateTo(HomeDirection.ToEditTraining(id))
    }

    override fun toAddTraining() {
        navigateTo(HomeDirection.ToAddTraining)
    }
}