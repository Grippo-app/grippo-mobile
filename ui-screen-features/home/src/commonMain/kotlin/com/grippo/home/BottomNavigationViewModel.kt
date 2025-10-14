package com.grippo.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.screen.api.BottomNavigationRouter
import kotlinx.coroutines.flow.firstOrNull

public class BottomNavigationViewModel(
    initial: BottomNavigationRouter,
    trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
) : BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
    BottomNavigationState(selected = BottomBarMenu.of(initial))
), BottomNavigationContract {

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
                    onResult = { navigateTo(BottomNavigationDirection.ToDraftTraining) }
                )

                dialogController.show(config)
            }
        }
    }

    override fun selectTab(origin: Int) {
        val selected = BottomBarMenu.entries.getOrNull(origin) ?: return

        val direction = when (selected) {
            BottomBarMenu.Trainings -> BottomNavigationDirection.Trainings
            BottomBarMenu.Statistics -> BottomNavigationDirection.Statistics
            BottomBarMenu.Profile -> BottomNavigationDirection.Profile
        }

        navigateTo(direction)

        update { it.copy(selected = selected) }
    }

    override fun onBack() {
        navigateTo(BottomNavigationDirection.Back)
    }

    override fun toExcludedMuscles() {
        navigateTo(BottomNavigationDirection.ToExcludedMuscles)
    }

    override fun toMissingEquipment() {
        navigateTo(BottomNavigationDirection.ToMissingEquipment)
    }

    override fun toWeightHistory() {
        navigateTo(BottomNavigationDirection.ToWeightHistory)
    }

    override fun toDebug() {
        navigateTo(BottomNavigationDirection.ToDebug)
    }

    override fun toEditTraining(id: String) {
        navigateTo(BottomNavigationDirection.ToEditTraining(id))
    }

    override fun toAddTraining() {
        navigateTo(BottomNavigationDirection.ToAddTraining)
    }
}