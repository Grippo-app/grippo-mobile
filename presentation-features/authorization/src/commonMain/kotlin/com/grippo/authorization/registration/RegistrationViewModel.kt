package com.grippo.authorization.registration

import com.grippo.core.BaseViewModel

internal class RegistrationViewModel :
    BaseViewModel<RegistrationState, RegistrationDirection, RegistrationLoader>(RegistrationState()),
    RegistrationContract {

    override fun saveCredentials(email: String, password: String) {
        update { it.copy(email = email, password = password) }
    }

    override fun saveName(name: String) {
        update { it.copy(name = name) }
    }
}