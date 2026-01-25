package com.grippo.authorization.auth.process

import androidx.compose.runtime.Immutable

@Immutable
internal interface AuthProcessContract {
    fun onClose()
    fun toRegistration(email: String?)
    fun toHome()
    fun toProfileCreation()
    fun onBack()

    @Immutable
    companion object Empty : AuthProcessContract {
        override fun onClose() {}
        override fun toRegistration(email: String?) {}
        override fun toHome() {}
        override fun toProfileCreation() {}
        override fun onBack() {}
    }
}
