package com.grippo.home.profile

internal interface ProfileContract {

    fun onLogoutClick()

    companion object Empty : ProfileContract {
        override fun onLogoutClick() {}
    }
}