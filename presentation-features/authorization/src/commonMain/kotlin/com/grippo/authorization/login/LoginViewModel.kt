package com.grippo.authorization.login

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.presentation.api.auth.models.EmailFormatState
import com.grippo.presentation.api.profile.models.PasswordFormatState

internal class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(LoginState()),
    LoginContract {

    override fun setEmail(value: String) {
        update { it.copy(email = EmailFormatState.of(value)) }
    }

    override fun setPassword(value: String) {
        update { it.copy(password = PasswordFormatState.of(value)) }
    }

    override fun login() {
        safeLaunch(loader = LoginLoader.LoginButton) {
            loginUseCase.execute(
                email = state.value.email.value,
                password = state.value.password.value
            )

            navigateTo(LoginDirection.Home)
        }
    }

    override fun register() {
        navigateTo(LoginDirection.Registration)
    }

    override fun back() {
        navigateTo(LoginDirection.Back)
    }
}