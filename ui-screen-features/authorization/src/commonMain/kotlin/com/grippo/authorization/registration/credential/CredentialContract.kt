package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Immutable

@Immutable
internal interface CredentialContract {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onEmailFocused()
    fun onPasswordFocused()
    fun onPasswordVisibilityChange(visible: Boolean)
    fun onEmailCleared()
    fun onEmailKeyboardNext()
    fun onPasswordKeyboardDone()
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : CredentialContract {
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onEmailFocused() {}
        override fun onPasswordFocused() {}
        override fun onPasswordVisibilityChange(visible: Boolean) {}
        override fun onEmailCleared() {}
        override fun onEmailKeyboardNext() {}
        override fun onPasswordKeyboardDone() {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}
