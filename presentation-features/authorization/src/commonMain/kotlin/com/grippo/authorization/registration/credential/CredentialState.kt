package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.EmailFormatState
import com.grippo.presentation.api.auth.models.PasswordFormatState

@Immutable
internal data class CredentialState(
    val email: EmailFormatState = EmailFormatState.of(""),
    val password: PasswordFormatState = PasswordFormatState.of(""),
)