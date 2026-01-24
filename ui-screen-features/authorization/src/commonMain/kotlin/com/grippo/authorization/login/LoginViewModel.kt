package com.grippo.authorization.login

import com.grippo.core.error.provider.AppError
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.services.apple.auth.AppleAuthException
import com.grippo.services.apple.auth.AppleAuthProvider
import com.grippo.services.apple.auth.AppleAuthUiContext
import com.grippo.services.google.auth.GoogleAuthException
import com.grippo.services.google.auth.GoogleAuthProvider
import com.grippo.services.google.auth.GoogleAuthUiContext

internal class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val googleAuthProvider: GoogleAuthProvider,
    private val appleAuthProvider: AppleAuthProvider,
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(
    LoginState(
        isGoogleLoginAvailable = googleAuthProvider.isSupported,
        isAppleLoginAvailable = appleAuthProvider.isSupported,
    )
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
            val hasProfile = loginUseCase.executeEmail(
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
                .onFailure {
                    val msg = (it as? GoogleAuthException)?.message ?: throw it
                    throw AppError.Expected(msg, description = null)
                }
                .getOrThrow()

            val hasProfile = loginUseCase.executeGoogle(
                token = googleAccount.token,
            )

            navigatePostLogin(hasProfile)
        }
    }

    override fun onLoginByAppleClick(context: AppleAuthUiContext) {
        safeLaunch(loader = LoginLoader.LoginByAppleButton) {
            val appleAccount = appleAuthProvider
                .getUiProvider(context)
                .signIn()
                .onFailure {
                    val msg = (it as? AppleAuthException)?.message ?: throw it
                    throw AppError.Expected(msg, description = null)
                }
                .getOrThrow()

            val hasProfile = loginUseCase.executeApple(
                token = appleAccount.token,
                code = appleAccount.authorizationCode,
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
