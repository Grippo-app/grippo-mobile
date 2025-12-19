package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class HomeViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState
), HomeContract {

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onProfileResult = {
                when (it) {
                    ProfileMenu.Muscles -> navigateTo(HomeDirection.ExcludedMuscles)
                    ProfileMenu.Equipment -> navigateTo(HomeDirection.MissingEquipment)
                }
            },
            onSettingsResult = {
                when (it) {
                    SettingsMenu.Debug -> navigateTo(HomeDirection.Debug)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(HomeDirection.Back)
    }
}
