package com.grippo.home.profile

internal interface ProfileContract {

    fun onLogoutClick()
    fun onMenuClick(menu: ProfileMenu)

    companion object Empty : ProfileContract {
        override fun onLogoutClick() {}
        override fun onMenuClick(menu: ProfileMenu) {}
    }
}