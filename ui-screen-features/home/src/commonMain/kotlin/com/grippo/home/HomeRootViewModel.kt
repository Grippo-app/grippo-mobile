package com.grippo.home

import com.grippo.core.foundation.BaseViewModel

public class HomeRootViewModel : BaseViewModel<HomeRootState, HomeRootDirection, HomeRootLoader>(
    HomeRootState
), HomeRootContract {

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

    override fun toDraftTraining() {
        navigateTo(HomeRootDirection.DraftTraining)
    }

    override fun toSettings() {
        navigateTo(HomeRootDirection.Settings)
    }
}
