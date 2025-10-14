package com.grippo.authorization.registration.credential

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface CredentialDirection : BaseDirection {
    data class Name(val email: String, val password: String) : CredentialDirection
    data object Back : CredentialDirection
}