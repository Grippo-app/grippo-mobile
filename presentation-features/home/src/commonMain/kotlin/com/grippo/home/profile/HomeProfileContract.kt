package com.grippo.home.profile

internal interface HomeProfileContract {

    fun onLogoutClick()
    fun onMenuClick(menu: HomeProfileMenu)

    companion object Empty : HomeProfileContract {
        override fun onLogoutClick() {}
        override fun onMenuClick(menu: HomeProfileMenu) {}
    }
}