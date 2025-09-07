package com.grippo.home

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.screen.api.BottomNavigationRouter
import kotlinx.coroutines.flow.onEach

public class BottomNavigationViewModel(
    initial: BottomNavigationRouter,
    trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
) : BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
    BottomNavigationState(selected = BottomBarMenu.of(initial))
), BottomNavigationContract {

    init {
        trainingFeature
            .getDraftTraining()
            .onEach(::provideDraftTraining)
            .safeLaunch()
    }

    private fun provideDraftTraining(value: SetTraining?) {
        val hasDraftTraining = value != null

        if (hasDraftTraining) {
            val config = DialogConfig.Confirmation(
                title = "asd",
                description = "asd",
                onResult = {}
            )

            dialogController.show(config)
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

    override fun toExerciseExamples() {
        navigateTo(BottomNavigationDirection.ToExerciseExamples)
    }

    override fun toDebug() {
        navigateTo(BottomNavigationDirection.ToDebug)
    }

    override fun toWorkout() {
        navigateTo(BottomNavigationDirection.ToWorkout)
    }

    override fun toSystemSettings() {
        navigateTo(BottomNavigationDirection.ToSystemSettings)
    }
}