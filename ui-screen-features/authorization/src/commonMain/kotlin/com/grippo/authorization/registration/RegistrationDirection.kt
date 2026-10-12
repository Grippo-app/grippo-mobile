package com.grippo.authorization.registration

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface RegistrationDirection : BaseDirection {
    data object CreateProfile : RegistrationDirection
    data object Home : RegistrationDirection
    data object Back : RegistrationDirection
}
