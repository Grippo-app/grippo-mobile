package com.grippo.authorization.login

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.authorization.models.CredentialField
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.authorization.analytics.AuthInputMapping
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
import com.grippo.toolkit.analytics.context.NavigationReason

internal class LoginViewModel(
    private val analytics: AuthAnalytics,
    private val loginUseCase: LoginUseCase,
    private val googleAuthProvider: GoogleAuthProvider,
    private val appleAuthProvider: AppleAuthProvider,
    initialState: LoginState = LoginState(),
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(
    initialState.copy(
        isGoogleLoginAvailable = googleAuthProvider.isSupported,
        isAppleLoginAvailable = appleAuthProvider.isSupported,
    )
), LoginContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Login, reason)
    }

    override fun onEmailChange(value: String) {
        val previousStatus = AuthInputMapping.status(state.value.email)
        val input = EmailFormatState.of(value)
        update { it.copy(email = input) }
        val status = AuthInputMapping.status(input)
        if (status != previousStatus) {
            analytics.trackEvent(AuthEvent.Login.InputChanged(CredentialField.Email, status))
        }
    }

    override fun onPasswordChange(value: String) {
        val previousStatus = AuthInputMapping.status(state.value.password)
        val input = PasswordFormatState.of(value)
        update { it.copy(password = input) }
        val status = AuthInputMapping.status(input)
        if (status != previousStatus) {
            analytics.trackEvent(AuthEvent.Login.InputChanged(CredentialField.Password, status))
        }
    }

    override fun onEmailFocused() {
        analytics.trackEvent(AuthEvent.Login.InputFocused(CredentialField.Email))
    }

    override fun onPasswordFocused() {
        analytics.trackEvent(AuthEvent.Login.InputFocused(CredentialField.Password))
    }

    override fun onPasswordVisibilityChange(visible: Boolean) {
        analytics.trackEvent(AuthEvent.Login.PasswordVisibilityChanged(visible))
    }

    override fun onEmailCleared() {
        analytics.trackEvent(AuthEvent.Login.InputCleared(CredentialField.Email))
    }

    override fun onEmailKeyboardNext() {
        analytics.trackEvent(
            AuthEvent.Login.KeyboardAction(CredentialField.Email, InputKeyboardAction.Next)
        )
    }

    override fun onPasswordKeyboardDone() {
        analytics.trackEvent(
            AuthEvent.Login.KeyboardAction(CredentialField.Password, InputKeyboardAction.Done)
        )
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

        analytics.trackEvent(AuthEvent.Login.Submit(Method.Email))

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

        analytics.trackEvent(AuthEvent.Login.Submit(Method.Google))

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

        analytics.trackEvent(AuthEvent.Login.Submit(Method.Apple))

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

        analytics.trackEvent(AuthEvent.Login.Register)

        val email = state.value.email.takeIf { it is EmailFormatState.Valid }?.display
        navigateTo(LoginDirection.Registration(email))
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Login.DismissRequested)
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
