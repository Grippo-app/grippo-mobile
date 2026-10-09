package com.grippo.profile.menu

import androidx.compose.runtime.Immutable
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu

@Immutable
internal interface ProfileMenuContract {
    fun onProfileMenuClick(menu: ProfileMenu)
    fun onSettingsMenuClick(menu: SettingsMenu)

    @Immutable
    companion object Empty : ProfileMenuContract {
        override fun onProfileMenuClick(menu: ProfileMenu) {}
        override fun onSettingsMenuClick(menu: SettingsMenu) {}
    }
}
