package com.grippo.authorization.login

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface LoginDirection : BaseDirection {
    data object Registration : LoginDirection
    data object Home : LoginDirection
    data object CreateProfile : LoginDirection
    data object Back : LoginDirection
}
