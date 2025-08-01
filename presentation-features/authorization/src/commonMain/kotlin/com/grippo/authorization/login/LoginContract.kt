package com.grippo.authorization.login

internal interface LoginContract {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onLoginClick()
    fun onRegisterClick()
    fun onBack()

    companion object Empty : LoginContract {
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onLoginClick() {}
        override fun onRegisterClick() {}
        override fun onBack() {}
    }
}