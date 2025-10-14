package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState

@Immutable
internal data class CredentialState(
    val email: EmailFormatState = EmailFormatState.of(""),
    val password: PasswordFormatState = PasswordFormatState.of(""),
)