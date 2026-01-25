package com.grippo.authorization.login

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface LoginDirection : BaseDirection {
    data class Registration(val email: String?) : LoginDirection
    data object Home : LoginDirection
    data object CreateProfile : LoginDirection
    data object Back : LoginDirection
}
