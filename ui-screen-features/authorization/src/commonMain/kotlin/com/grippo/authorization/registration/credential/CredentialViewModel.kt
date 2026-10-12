package com.grippo.authorization.registration.credential

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.authorization.models.CredentialField
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.authorization.analytics.AuthInputMapping
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.data.features.api.authorization.RegisterUseCase
import com.grippo.toolkit.analytics.context.NavigationReason

internal class CredentialViewModel(
    email: String?,
    private val analytics: AuthAnalytics,
    private val registerUseCase: RegisterUseCase
) : BaseViewModel<CredentialState, CredentialDirection, CredentialLoader>(
    CredentialState(
        email = EmailFormatState.of(email)
    )
), CredentialContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Registration, reason)
    }

    override fun onEmailChange(value: String) {
        val previousStatus = AuthInputMapping.status(state.value.email)
        val input = EmailFormatState.of(value)
        update { it.copy(email = input) }
        val status = AuthInputMapping.status(input)
        if (status != previousStatus) {
            analytics.trackEvent(AuthEvent.Registration.InputChanged(CredentialField.Email, status))
        }
    }

    override fun onPasswordChange(value: String) {
        val previousStatus = AuthInputMapping.status(state.value.password)
        val input = PasswordFormatState.of(value)
        update { it.copy(password = input) }
        val status = AuthInputMapping.status(input)
        if (status != previousStatus) {
            analytics.trackEvent(AuthEvent.Registration.InputChanged(CredentialField.Password, status))
        }
    }

    override fun onEmailFocused() {
        analytics.trackEvent(AuthEvent.Registration.InputFocused(CredentialField.Email))
    }

    override fun onPasswordFocused() {
        analytics.trackEvent(AuthEvent.Registration.InputFocused(CredentialField.Password))
    }

    override fun onPasswordVisibilityChange(visible: Boolean) {
        analytics.trackEvent(AuthEvent.Registration.PasswordVisibilityChanged(visible))
    }

    override fun onEmailCleared() {
        analytics.trackEvent(AuthEvent.Registration.InputCleared(CredentialField.Email))
    }

    override fun onEmailKeyboardNext() {
        analytics.trackEvent(
            AuthEvent.Registration.KeyboardAction(CredentialField.Email, InputKeyboardAction.Next)
        )
    }

    override fun onPasswordKeyboardDone() {
        analytics.trackEvent(
            AuthEvent.Registration.KeyboardAction(CredentialField.Password, InputKeyboardAction.Done)
        )
    }

    override fun onNextClick() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        val snapshot = state.value
        val email = snapshot.email
        val password = snapshot.password
        if (email !is EmailFormatState.Valid || password !is PasswordFormatState.Valid) {
            return
        }

        analytics.trackEvent(AuthEvent.Registration.Submit)

        safeLaunch(loader = CredentialLoader.RegisterButton) {
            val hasProfile = analytics.trackOperation(AuthOperation.Register) {
                registerUseCase.createAccount(
                    email = email.value,
                    password = password.value,
                )
            }

            val destination = if (hasProfile) {
                CredentialDirection.Home
            } else {
                CredentialDirection.CreateProfile
            }
            navigateTo(destination)
        }
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Registration.Back)
        navigateTo(CredentialDirection.Back)
    }
}
