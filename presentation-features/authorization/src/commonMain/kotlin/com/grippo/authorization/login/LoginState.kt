package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.EmailFormatState
import com.grippo.state.formatters.PasswordFormatState

@Immutable
internal data class LoginState(
    val email: EmailFormatState = EmailFormatState.of("grippo@mail.com"),
    val password: PasswordFormatState = PasswordFormatState.of("qwerty123"),
)