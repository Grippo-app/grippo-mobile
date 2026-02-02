package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState

@Immutable
internal data class LoginState(
    val email: EmailFormatState = EmailFormatState.Empty(),
    val password: PasswordFormatState = PasswordFormatState.Empty(),
    val isGoogleLoginAvailable: Boolean = false,
    val isAppleLoginAvailable: Boolean = false,
)
