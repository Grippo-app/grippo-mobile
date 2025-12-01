package com.grippo.authorization.registration.credential

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.data.features.api.authorization.RegisterUseCase

internal class CredentialViewModel(
    private val registerUseCase: RegisterUseCase
) : BaseViewModel<CredentialState, CredentialDirection, CredentialLoader>(CredentialState()),
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

        safeLaunch(loader = CredentialLoader.RegisterButton) {
            val hasProfile = registerUseCase.execute(
                email = email.value,
                password = password.value
            )

            if (hasProfile) {
                navigateTo(CredentialDirection.Home)
            } else {
                navigateTo(CredentialDirection.CreateProfile)
            }
        }
    }

    override fun onBack() {
        navigateTo(CredentialDirection.Back)
    }
}
