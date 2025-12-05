package com.grippo.authorization.login

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.services.google.auth.GoogleAuthProvider
import com.grippo.services.google.auth.GoogleAuthUiContext

internal class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val googleAuthProvider: GoogleAuthProvider,
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(
    LoginState(isGoogleLoginAvailable = googleAuthProvider.isSupported)
),
    LoginContract {

    override fun onEmailChange(value: String) {
        update { it.copy(email = EmailFormatState.of(value)) }
    }

    override fun onPasswordChange(value: String) {
        update { it.copy(password = PasswordFormatState.of(value)) }
    }

    override fun onLoginByEmailClick() {
        safeLaunch(loader = LoginLoader.LoginByEmailButton) {
            val loginState = state.value
            val hasProfile = loginUseCase.execute(
                email = loginState.email.value.orEmpty(),
                password = loginState.password.value.orEmpty()
            )

            navigatePostLogin(hasProfile)
        }
    }

    override fun onLoginByGoogleClick(context: GoogleAuthUiContext) {
        safeLaunch(loader = LoginLoader.LoginByGoogleButton) {
            val googleAccount = googleAuthProvider
                .getUiProvider(context)
                .signIn()

            val hasProfile = loginUseCase.execute(
                token = googleAccount.token,
            )

            navigatePostLogin(hasProfile)
        }
    }

    override fun onRegisterClick() {
        navigateTo(LoginDirection.Registration)
    }

    override fun onBack() {
        navigateTo(LoginDirection.Back)
    }

    private fun navigatePostLogin(hasProfile: Boolean) {
        if (hasProfile) {
            navigateTo(LoginDirection.Home)
        } else {
            navigateTo(LoginDirection.CreateProfile)
        }
    }
}
