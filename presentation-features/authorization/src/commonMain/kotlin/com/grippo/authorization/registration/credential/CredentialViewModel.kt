package com.grippo.authorization.registration.credential

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.auth.models.Email
import com.grippo.presentation.api.auth.models.Password

internal class CredentialViewModel :
    BaseViewModel<CredentialState, CredentialDirection, CredentialLoader>(CredentialState()),
    CredentialContract {
    override fun setEmail(value: String) {
        update { it.copy(email = Email.of(value)) }
    }

    override fun setPassword(value: String) {
        update { it.copy(password = Password.of(value)) }
    }

    override fun next() {
    }
}