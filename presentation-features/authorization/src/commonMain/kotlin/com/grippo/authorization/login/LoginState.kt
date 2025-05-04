package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.Email
import com.grippo.presentation.api.auth.models.Password

@Immutable
internal data class LoginState(
    val email: Email = Email.of(""),
    val password: Password = Password.of(""),
)