package com.grippo.authorization.login

import androidx.compose.runtime.Immutable

@Immutable
internal interface LoginContract {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onLoginByEmailClick()
    fun onLoginByGoogleClick()
    fun onRegisterClick()
    fun onBack()

    @Immutable
    companion object Empty : LoginContract {
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onLoginByEmailClick() {}
        override fun onLoginByGoogleClick() {}
        override fun onRegisterClick() {}
        override fun onBack() {}
    }
}