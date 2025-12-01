package com.grippo.authorization.auth.process

import androidx.compose.runtime.Immutable

@Immutable
internal interface AuthProcessContract {
    fun onClose()
    fun toRegistration()
    fun toHome()
    fun toProfileCreation()
    fun onBack()

    @Immutable
    companion object Empty : AuthProcessContract {
        override fun onClose() {}
        override fun toRegistration() {}
        override fun toHome() {}
        override fun toProfileCreation() {}
        override fun onBack() {}
    }
}
