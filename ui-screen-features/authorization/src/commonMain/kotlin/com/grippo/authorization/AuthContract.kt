package com.grippo.authorization

import androidx.compose.runtime.Immutable

@Immutable
internal interface AuthContract {
    fun onBack()
    fun toOnboarding()
    fun toAuthProcess()
    fun toHome()
    fun toProfileCreation()

    @Immutable
    companion object Empty : AuthContract {
        override fun onBack() {}
        override fun toOnboarding() {}
        override fun toAuthProcess() {}
        override fun toHome() {}
        override fun toProfileCreation() {}
    }
}
