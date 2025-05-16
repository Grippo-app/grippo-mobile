package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.EmailFormatState
import com.grippo.presentation.api.auth.models.PasswordFormatState

@Immutable
internal data class LoginState(
    val email: EmailFormatState = EmailFormatState.of("grippo@mail.com"),
    val password: PasswordFormatState = PasswordFormatState.of("qwerty123"),
)