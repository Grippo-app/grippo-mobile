package com.grippo.authorization.registration

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState

@Immutable
internal data class RegistrationState(
    val email: EmailFormatState = EmailFormatState.Empty(),
    val password: PasswordFormatState = PasswordFormatState.Empty()
)
