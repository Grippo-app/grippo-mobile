package com.grippo.authorization.login

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.data.features.api.authorization.LoginUseCase
import kotlinx.coroutines.delay

internal class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(LoginState()),
    LoginContract {

    override fun onEmailChange(value: String) {
        update { it.copy(email = EmailFormatState.of(value)) }
    }

    override fun onPasswordChange(value: String) {
        update { it.copy(password = PasswordFormatState.of(value)) }
    }

    override fun onLoginByEmailClick() {
        safeLaunch(loader = LoginLoader.LoginByEmailButton) {
            val hasProfile = loginUseCase.execute(
                email = state.value.email.value ?: "",
                password = state.value.password.value ?: ""
            )

            if (hasProfile) {
                navigateTo(LoginDirection.Home)
            } else {
                navigateTo(LoginDirection.CreateProfile)
            }
        }
    }

    override fun onLoginByGoogleClick() {
        safeLaunch(loader = LoginLoader.LoginByGoogleButton) {
            delay(1000)
        }
    }

    override fun onRegisterClick() {
        navigateTo(LoginDirection.Registration)
    }

    override fun onBack() {
        navigateTo(LoginDirection.Back)
    }
}
