package com.grippo.dialog.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ProfileActivityMenu

@Immutable
internal interface ProfileContract {
    fun onLogoutClick()
    fun onActivityMenuClick(menu: ProfileActivityMenu)
    fun onBack()

    @Immutable
    companion object Empty : ProfileContract {
        override fun onLogoutClick() {}
        override fun onActivityMenuClick(menu: ProfileActivityMenu) {}
        override fun onBack() {}
    }
}