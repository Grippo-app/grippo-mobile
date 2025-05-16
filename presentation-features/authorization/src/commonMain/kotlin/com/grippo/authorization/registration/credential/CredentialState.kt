package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.EmailState
import com.grippo.presentation.api.auth.models.PasswordState

@Immutable
internal data class CredentialState(
    val email: EmailState = EmailState.of(""),
    val password: PasswordState = PasswordState.of(""),
)