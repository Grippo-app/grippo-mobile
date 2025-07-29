package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Immutable
import com.grippo.state.auth.EmailFormatState
import com.grippo.state.profile.PasswordFormatState

@Immutable
internal data class CredentialState(
    val email: EmailFormatState = EmailFormatState.of(""),
    val password: PasswordFormatState = PasswordFormatState.of(""),
)