package com.grippo.home.profile

internal interface HomeProfileContract {
    fun onLogoutClick()
    fun onDebugClick()
    fun onStartWorkoutClick()
    fun onActivityMenuClick(menu: HomeProfileActivityMenu)
    fun onSettingsMenuClick(menu: HomeProfileSettingsMenu)
    fun back()

    companion object Empty : HomeProfileContract {
        override fun onLogoutClick() {}
        override fun onActivityMenuClick(menu: HomeProfileActivityMenu) {}
        override fun onSettingsMenuClick(menu: HomeProfileSettingsMenu) {}
        override fun back() {}
        override fun onDebugClick() {}
        override fun onStartWorkoutClick() {}
    }
}