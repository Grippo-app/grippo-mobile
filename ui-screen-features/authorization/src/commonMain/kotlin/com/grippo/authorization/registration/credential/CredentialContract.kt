package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Immutable

@Immutable
internal interface CredentialContract {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : CredentialContract {
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}