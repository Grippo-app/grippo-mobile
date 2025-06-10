package com.grippo.authorization.registration.credential

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.auth.models.EmailFormatState
import com.grippo.presentation.api.auth.models.PasswordFormatState

internal class CredentialViewModel :
    BaseViewModel<CredentialState, CredentialDirection, CredentialLoader>(CredentialState()),
    CredentialContract {
    override fun setEmail(value: String) {
        update { it.copy(email = EmailFormatState.of(value)) }
    }

    override fun setPassword(value: String) {
        update { it.copy(password = PasswordFormatState.of(value)) }
    }

    override fun next() {
        val direction = CredentialDirection.Name(
            email = state.value.email.value,
            password = state.value.password.value
        )
        navigateTo(direction)
    }

    override fun back() {
        navigateTo(CredentialDirection.Back)
    }
}