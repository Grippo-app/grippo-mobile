package com.grippo.dialog.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu

@Immutable
internal interface ProfileContract {
    fun onLogoutClick()
    fun onProfileMenuClick(menu: ProfileMenu)
    fun onSettingsMenuClick(menu: SettingsMenu)
    fun onBack()

    @Immutable
    companion object Empty : ProfileContract {
        override fun onLogoutClick() {}
        override fun onProfileMenuClick(menu: ProfileMenu) {}
        override fun onSettingsMenuClick(menu: SettingsMenu) {}
        override fun onBack() {}
    }
}