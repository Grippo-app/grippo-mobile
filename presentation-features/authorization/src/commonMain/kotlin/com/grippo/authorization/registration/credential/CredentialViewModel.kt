package com.grippo.authorization.registration.credential

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.auth.models.EmailState
import com.grippo.presentation.api.auth.models.PasswordState

internal class CredentialViewModel :
    BaseViewModel<CredentialState, CredentialDirection, CredentialLoader>(CredentialState()),
    CredentialContract {
    override fun setEmail(value: String) {
        update { it.copy(email = EmailState.of(value)) }
    }

    override fun setPassword(value: String) {
        update { it.copy(password = PasswordState.of(value)) }
    }

    override fun next() {
        val direction = CredentialDirection.Name(
            email = state.value.email.value,
            password = state.value.password.value
        )
        navigateTo(direction)
    }
}