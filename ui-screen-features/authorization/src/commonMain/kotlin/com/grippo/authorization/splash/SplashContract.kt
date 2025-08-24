package com.grippo.authorization.splash

internal interface SplashContract {
    fun onBack()

    companion object Empty : SplashContract {
        override fun onBack() {}
    }
}