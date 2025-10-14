package com.grippo.authorization.registration.credential

import com.grippo.core.foundation.BaseViewModel
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
        val email = (state.value.email as? EmailFormatState.Valid) ?: return
        val password = (state.value.password as? PasswordFormatState.Valid) ?: return

        val direction = CredentialDirection.Name(
            email = email.value,
            password = password.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(CredentialDirection.Back)
    }
}