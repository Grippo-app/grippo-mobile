package com.grippo.authorization.splash

import androidx.compose.runtime.Immutable

@Immutable
internal interface SplashContract {
    fun onBack()

    @Immutable
    companion object Empty : SplashContract {
        override fun onBack() {}
    }
}