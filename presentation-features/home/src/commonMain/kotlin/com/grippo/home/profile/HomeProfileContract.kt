package com.grippo.home.profile

internal interface HomeProfileContract {
    fun onLogoutClick()
    fun onDebugClick()
    fun onMenuClick(menu: HomeProfileMenu)
    fun back()

    companion object Empty : HomeProfileContract {
        override fun onLogoutClick() {}
        override fun onMenuClick(menu: HomeProfileMenu) {}
        override fun back() {}
        override fun onDebugClick() {}
    }
}