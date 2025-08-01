package com.grippo.authorization.registration.credential

internal interface CredentialContract {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onNextClick()
    fun onBack()

    companion object Empty : CredentialContract {
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}