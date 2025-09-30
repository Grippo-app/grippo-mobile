package com.grippo.home.profile

internal interface HomeProfileContract {
    fun onLogoutClick()
    fun onStartTrainingClick()
    fun onActivityMenuClick(menu: HomeProfileActivityMenu)
    fun onSettingsMenuClick(menu: HomeProfileSettingsMenu)
    fun onBack()

    companion object Empty : HomeProfileContract {
        override fun onLogoutClick() {}
        override fun onActivityMenuClick(menu: HomeProfileActivityMenu) {}
        override fun onSettingsMenuClick(menu: HomeProfileSettingsMenu) {}
        override fun onStartTrainingClick() {}
        override fun onBack() {}
    }
}