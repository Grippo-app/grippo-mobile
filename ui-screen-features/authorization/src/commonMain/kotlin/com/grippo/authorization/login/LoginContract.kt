package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.services.google.auth.GoogleAuthUiContext

@Immutable
internal interface LoginContract {
    fun onEmailChange(value: String)
    fun onPasswordChange(value: String)
    fun onLoginByEmailClick()
    fun onLoginByGoogleClick(context: GoogleAuthUiContext)
    fun onRegisterClick()
    fun onBack()

    @Immutable
    companion object Empty : LoginContract {
        override fun onEmailChange(value: String) {}
        override fun onPasswordChange(value: String) {}
        override fun onLoginByEmailClick() {}
        override fun onLoginByGoogleClick(context: GoogleAuthUiContext) {}
        override fun onRegisterClick() {}
        override fun onBack() {}
    }
}
