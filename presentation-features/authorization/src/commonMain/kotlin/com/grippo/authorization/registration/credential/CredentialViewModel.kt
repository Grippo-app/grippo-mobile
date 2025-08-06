package com.grippo.authorization.registration.credential

import com.grippo.core.BaseViewModel
import com.grippo.state.formatters.EmailFormatState
import com.grippo.state.formatters.PasswordFormatState

internal class CredentialViewModel :
    BaseViewModel<CredentialState, CredentialDirection, CredentialLoader>(CredentialState()),
    CredentialContract {
    override fun onEmailChange(value: String) {
        update { it.copy(email = EmailFormatState.of(value)) }
    }

    override fun onPasswordChange(value: String) {
        update { it.copy(password = PasswordFormatState.of(value)) }
    }

    override fun onNextClick() {
        val direction = CredentialDirection.Name(
            email = state.value.email.value,
            password = state.value.password.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(CredentialDirection.Back)
    }
}