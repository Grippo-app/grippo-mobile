package com.grippo.dialog.profile

import com.grippo.core.state.profile.ProfileActivityMenu

internal interface ProfileContract {
    fun onLogoutClick()
    fun onActivityMenuClick(menu: ProfileActivityMenu)
    fun onBack()

    companion object Empty : ProfileContract {
        override fun onLogoutClick() {}
        override fun onActivityMenuClick(menu: ProfileActivityMenu) {}
        override fun onBack() {}
    }
}