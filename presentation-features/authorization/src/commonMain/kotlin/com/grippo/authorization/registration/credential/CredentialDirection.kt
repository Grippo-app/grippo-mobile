package com.grippo.authorization.registration.credential

import com.grippo.core.models.BaseDirection

internal sealed interface CredentialDirection : BaseDirection {
    data class Name(val email: String, val password: String) : CredentialDirection
}