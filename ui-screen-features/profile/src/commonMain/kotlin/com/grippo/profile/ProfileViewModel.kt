package com.grippo.profile

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.screen.api.ProfileRouter

public class ProfileViewModel :
    BaseViewModel<ProfileState, ProfileDirection, ProfileLoader>(ProfileState), ProfileContract {

    override fun onProfileMenuClick(menu: ProfileMenu) {
        val router = when (menu) {
            ProfileMenu.Body -> ProfileRouter.Body
            ProfileMenu.Equipment -> ProfileRouter.Equipments
            ProfileMenu.Muscles -> ProfileRouter.Muscles
            ProfileMenu.Experience -> ProfileRouter.Experience
            ProfileMenu.Goal -> ProfileRouter.Goal
        }
        navigateTo(ProfileDirection.Open(router))
    }

    override fun onSettingsMenuClick(menu: SettingsMenu) {
        val direction = when (menu) {
            SettingsMenu.Debug -> ProfileDirection.Debug
            SettingsMenu.Settings -> ProfileDirection.Open(ProfileRouter.Settings)
            SettingsMenu.Social -> ProfileDirection.Open(ProfileRouter.Social)
        }
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(ProfileDirection.Back)
    }
}
