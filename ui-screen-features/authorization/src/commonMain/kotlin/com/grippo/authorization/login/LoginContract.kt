package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.services.apple.auth.AppleAuthUiContext
import com.grippo.services.google.auth.GoogleAuthUiContext

@Immutable
internal interface LoginContract {
    fun onAnalyticsConsentChange(granted: Boolean)
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onEmailFocused()
    fun onPasswordFocused()
    fun onPasswordVisibilityChange(visible: Boolean)
    fun onEmailCleared()
    fun onEmailKeyboardNext()
    fun onPasswordKeyboardDone()
    fun onLoginByEmailClick()
    fun onLoginByGoogleClick(context: GoogleAuthUiContext)
    fun onLoginByAppleClick(context: AppleAuthUiContext)
    fun onRegisterClick()
    fun onBack()

    @Immutable
    companion object Empty : LoginContract {
        override fun onAnalyticsConsentChange(granted: Boolean) {}
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onEmailFocused() {}
        override fun onPasswordFocused() {}
        override fun onPasswordVisibilityChange(visible: Boolean) {}
        override fun onEmailCleared() {}
        override fun onEmailKeyboardNext() {}
        override fun onPasswordKeyboardDone() {}
        override fun onLoginByEmailClick() {}
        override fun onLoginByGoogleClick(context: GoogleAuthUiContext) {}
        override fun onLoginByAppleClick(context: AppleAuthUiContext) {}
        override fun onRegisterClick() {}
        override fun onBack() {}
    }
}
