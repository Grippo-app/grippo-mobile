package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.services.apple.auth.AppleAuthUiContext
import com.grippo.services.google.auth.GoogleAuthUiContext

@Immutable
internal interface LoginContract {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onLoginByEmailClick()
    fun onLoginByGoogleClick(context: GoogleAuthUiContext)
    fun onLoginByAppleClick(context: AppleAuthUiContext)
    fun onRegisterClick()
    fun onBack()

    @Immutable
    companion object Empty : LoginContract {
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onLoginByEmailClick() {}
        override fun onLoginByGoogleClick(context: GoogleAuthUiContext) {}
        override fun onLoginByAppleClick(context: AppleAuthUiContext) {}
        override fun onRegisterClick() {}
        override fun onBack() {}
    }
}
