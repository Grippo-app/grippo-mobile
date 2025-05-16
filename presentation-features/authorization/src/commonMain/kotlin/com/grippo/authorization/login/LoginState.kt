package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.EmailState
import com.grippo.presentation.api.auth.models.PasswordState

@Immutable
internal data class LoginState(
    val email: EmailState = EmailState.of("grippo@mail.com"),
    val password: PasswordState = PasswordState.of("qwerty123"),
)