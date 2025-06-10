package com.grippo.authorization.splash

internal interface SplashContract {
    fun back()

    companion object Empty : SplashContract {
        override fun back() {}
    }
}