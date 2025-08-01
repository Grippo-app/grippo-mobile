package com.grippo.authorization.login

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.state.auth.EmailFormatState
import com.grippo.state.profile.PasswordFormatState

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

    override fun onLoginClick() {
        safeLaunch(loader = LoginLoader.LoginButton) {
            loginUseCase.execute(
                email = state.value.email.value,
                password = state.value.password.value
            )

            navigateTo(LoginDirection.Home)
        }
    }

    override fun onRegisterClick() {
        navigateTo(LoginDirection.Registration)
    }

    override fun onBack() {
        navigateTo(LoginDirection.Back)
    }
}