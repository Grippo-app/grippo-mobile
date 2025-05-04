package com.grippo.authorization.registration.credential

import com.grippo.core.models.BaseDirection

internal sealed interface CredentialDirection : BaseDirection {
    data object Name : CredentialDirection
}