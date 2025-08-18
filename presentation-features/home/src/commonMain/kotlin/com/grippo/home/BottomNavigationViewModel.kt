package com.grippo.home

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter

public class BottomNavigationViewModel(
    initial: BottomNavigationRouter
) : BaseViewModel<BottomNavigationState, BottomNavigationDirection, BottomNavigationLoader>(
    BottomNavigationState(selected = BottomBarMenu.of(initial))
), BottomNavigationContract {

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

    override fun toExerciseLibrary() {
        navigateTo(BottomNavigationDirection.ToExerciseLibrary)
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