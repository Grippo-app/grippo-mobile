package com.grippo.authorization.registration

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class RegistrationState(
    val email: EmailFormatState = EmailFormatState.Empty(),
    @Transient
    val password: PasswordFormatState = PasswordFormatState.Empty()
)
