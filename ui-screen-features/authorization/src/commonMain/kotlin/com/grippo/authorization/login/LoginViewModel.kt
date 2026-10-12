package com.grippo.authorization.login

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.Method
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
    private val analytics: AuthAnalytics.Authorization,
    private val loginUseCase: LoginUseCase,
    private val googleAuthProvider: GoogleAuthProvider,
    private val appleAuthProvider: AppleAuthProvider,
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(
    LoginState(
        isGoogleLoginAvailable = googleAuthProvider.isSupported,
        isAppleLoginAvailable = appleAuthProvider.isSupported,
    )
), LoginContract {

    override fun onEmailChange(value: String) {
        update { it.copy(email = EmailFormatState.of(value)) }
    }

    override fun onPasswordChange(value: String) {
        update { it.copy(password = PasswordFormatState.of(value)) }
    }

    override fun onLoginByEmailClick() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        val snapshot = state.value
        val email = snapshot.email
        val password = snapshot.password
        if (email !is EmailFormatState.Valid || password !is PasswordFormatState.Valid) {
            return
        }

        safeLaunch(loader = LoginLoader.LoginByEmailButton) {
            val hasProfile = analytics.trackOperation(AuthOperation.Login(Method.Email)) {
                loginUseCase.loginEmail(
                    email = email.value,
                    password = password.value,
                )
            }

            navigateAfterLogin(hasProfile)
        }
    }

    override fun onLoginByGoogleClick(context: GoogleAuthUiContext) {
        if (loaders.value.isNotEmpty()) {
            return
        }

        safeLaunch(
            loader = LoginLoader.LoginByGoogleButton,
            mapError = ::mapAuthError,
        ) {
            val hasProfile = analytics.trackOperation(AuthOperation.Login(Method.Google)) {
                val account = googleAuthProvider
                    .getUiProvider(context)
                    .signIn()
                    .getOrThrow()
                loginUseCase.loginGoogle(token = account.token)
            }

            navigateAfterLogin(hasProfile)
        }
    }

    override fun onLoginByAppleClick(context: AppleAuthUiContext) {
        if (loaders.value.isNotEmpty()) {
            return
        }

        safeLaunch(
            loader = LoginLoader.LoginByAppleButton,
            mapError = ::mapAuthError,
        ) {
            val hasProfile = analytics.trackOperation(AuthOperation.Login(Method.Apple)) {
                val account = appleAuthProvider
                    .getUiProvider(context)
                    .signIn()
                    .getOrThrow()
                loginUseCase.loginApple(code = account.authorizationCode)
            }

            navigateAfterLogin(hasProfile)
        }
    }

    override fun onRegisterClick() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        val email = state.value.email.takeIf { it is EmailFormatState.Valid }?.display
        navigateTo(LoginDirection.Registration(email))
    }

    override fun onBack() {
        navigateTo(LoginDirection.Back)
    }

    private fun navigateAfterLogin(hasProfile: Boolean) {
        val destination = if (hasProfile) {
            LoginDirection.Home
        } else {
            LoginDirection.CreateProfile
        }
        navigateTo(destination)
    }

    private fun mapAuthError(error: Throwable): Throwable = when (error) {
        is GoogleAuthException -> AppError.Expected(error.message, description = null)
        is AppleAuthException -> AppError.Expected(error.message, description = null)
        else -> error
    }
}
