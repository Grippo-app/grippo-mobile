package com.grippo.authorization.registration.credential

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.data.features.api.authorization.RegisterUseCase

internal class CredentialViewModel(
    email: String?,
    private val analytics: AuthAnalytics.Authorization,
    private val registerUseCase: RegisterUseCase
) : BaseViewModel<CredentialState, CredentialDirection, CredentialLoader>(
    CredentialState(
        email = EmailFormatState.of(email)
    )
), CredentialContract {

    override fun onEmailChange(value: String) {
        update { it.copy(email = EmailFormatState.of(value)) }
    }

    override fun onPasswordChange(value: String) {
        update { it.copy(password = PasswordFormatState.of(value)) }
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
        navigateTo(CredentialDirection.Back)
    }
}
