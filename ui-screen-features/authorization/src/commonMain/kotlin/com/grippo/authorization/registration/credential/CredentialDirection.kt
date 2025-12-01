package com.grippo.authorization.registration.credential

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface CredentialDirection : BaseDirection {
    data object CreateProfile : CredentialDirection
    data object Home : CredentialDirection
    data object Back : CredentialDirection
}
