package com.grippo.authorization.login

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.presentation.api.auth.models.Email
import com.grippo.presentation.api.auth.models.Password
import kotlinx.coroutines.delay

internal class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(LoginState()),
    LoginContract {

    override fun setEmail(value: String) {
        update { it.copy(email = Email.of(value)) }
    }

    override fun setPassword(value: String) {
        update { it.copy(password = Password.of(value)) }
    }

    override fun login() {
        safeLaunch(loader = LoginLoader.LoginButton) {
            delay(1500)
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
}