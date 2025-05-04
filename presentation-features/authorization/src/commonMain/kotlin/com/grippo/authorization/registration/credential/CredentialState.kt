package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.Email
import com.grippo.presentation.api.auth.models.Password

@Immutable
internal data class CredentialState(
    val email: Email = Email.of(""),
    val password: Password = Password.of(""),
)